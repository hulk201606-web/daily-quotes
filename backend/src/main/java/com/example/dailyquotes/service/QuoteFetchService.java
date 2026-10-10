package com.example.dailyquotes.service;

import com.example.dailyquotes.client.ChineseQuoteClient;
import com.example.dailyquotes.client.InternetQuoteClient;
import com.example.dailyquotes.client.RemoteQuote;
import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.QuoteFetchLog;
import com.example.dailyquotes.domain.QuoteOrigin;
import com.example.dailyquotes.dto.FetchResultResponse;
import com.example.dailyquotes.dto.ImportResultResponse;
import com.example.dailyquotes.dto.TranslateResultResponse;
import com.example.dailyquotes.exception.InternetQuoteException;
import com.example.dailyquotes.mapper.QuoteFetchLogMapper;
import com.example.dailyquotes.mapper.QuoteMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 每日名言采集服务：从互联网公开名言库拉取不重复的名言并持久化。
 *
 * <p>核心职责：
 * <ol>
 *   <li>限量控制——定时任务（SCHEDULED）保证「每日仅成功采集一次」；手动「立即采集」（MANUAL）不受限，可随时补充；</li>
 *   <li>去重入库——对每条远程名言计算指纹；若与本次批次或历史库重复则跳过并继续采集补充，直至凑满当日配额；</li>
 *   <li>失败容错——单次拉取失败时按最大尝试次数重试，全部失败则记录 FAILED 日志并抛出业务异常；</li>
 *   <li>可追溯——每次任务结束（成功或失败）都会写入一条采集日志。</li>
 * </ol>
 * 互联网来源的名言由自动填充处理器记录 {@code last_fetched_date}。</p>
 */
@Service
public class QuoteFetchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteFetchService.class);

    private static final int MAX_ATTEMPTS = 50;

    /** 每次拉取时在「剩余所需」基础上多拉的倍数，用于对冲重复率、减少空转重试。 */
    private static final int FETCH_PAGE_FACTOR = 3;

    private final InternetQuoteClient internetQuoteClient;
    private final QuoteMapper quoteMapper;
    private final QuoteFetchLogMapper quoteFetchLogMapper;
    private final TranslationService translationService;
    private final ChineseQuoteImporter chineseQuoteImporter;
    private final ChineseQuoteClient chineseQuoteClient;
    private final int dailyLimit;
    private final int maxPageSize;
    private final boolean translateEnabled;
    private final int chineseOnlineLimit;
    private final long translateIntervalMs;
    private final int maxConsecutiveFailures;

    public QuoteFetchService(InternetQuoteClient internetQuoteClient,
                             QuoteMapper quoteMapper,
                             QuoteFetchLogMapper quoteFetchLogMapper,
                             TranslationService translationService,
                             ChineseQuoteImporter chineseQuoteImporter,
                             ChineseQuoteClient chineseQuoteClient,
                             @Value("${app.quote-fetch.daily-limit:100}") int dailyLimit,
                             @Value("${app.quote-fetch.max-page-size:10}") int maxPageSize,
                             @Value("${app.quote-fetch.translate-enabled:true}") boolean translateEnabled,
                             @Value("${app.chinese-quotes.online-limit:20}") int chineseOnlineLimit,
                             @Value("${app.translate.interval-ms:350}") long translateIntervalMs,
                             @Value("${app.translate.max-consecutive-failures:20}") int maxConsecutiveFailures) {
        this.internetQuoteClient = internetQuoteClient;
        this.quoteMapper = quoteMapper;
        this.quoteFetchLogMapper = quoteFetchLogMapper;
        this.translationService = translationService;
        this.chineseQuoteImporter = chineseQuoteImporter;
        this.chineseQuoteClient = chineseQuoteClient;
        this.dailyLimit = dailyLimit;
        // 远程名言接口对单次拉取条数有上限（如 dummyjson 在 limit>10 时返回空数组），
        // 超出上限会导致整页为空、无法落库，因此此处强制收敛到安全上限内。
        this.maxPageSize = Math.max(1, maxPageSize);
        this.translateEnabled = translateEnabled;
        this.chineseOnlineLimit = Math.max(0, chineseOnlineLimit);
        this.translateIntervalMs = Math.max(0, translateIntervalMs);
        this.maxConsecutiveFailures = Math.max(1, maxConsecutiveFailures);
    }

    /**
     * 执行一次名言采集流程。
     *
     * <p>流程（中文优先，两阶段采集）：① 限量判断——仅定时任务（SCHEDULED）在「当日已有 SUCCESS 采集记录」时跳过；
 *    手动（MANUAL）不受日限额约束，可随时重新采集补充（重复由指纹去重兜底，不会写重）；
     * ② 循环调用互联网接口，按指纹去重；遇到重复条目即跳过并继续采集补充，
 *    每次按预估重复率多拉一批，直至凑满 {@code dailyLimit} 条或达到最大尝试次数；
     * ③ 批量写入去重后的名言；④ 写入采集日志（SUCCESS / FAILED）。
     * 若全部拉取失败且有错误，则记录 FAILED 日志并向上抛出 {@link InternetQuoteException}。</p>
     *
     * @param fetchType 触发方式：SCHEDULED（定时）或 MANUAL（手动）
     * @return 本次采集结果（新增数、跳过数、尝试次数）
     */
    @Transactional
    public FetchResultResponse fetchNow(String fetchType) {
        LOGGER.info("[DB采集] 开始采集名言，类型={}，目标 {} 条（中文优先，不足再采集外文并翻译为中文），最大尝试 {} 次",
                fetchType, dailyLimit, MAX_ATTEMPTS);
        LocalDate today = LocalDate.now();

        // 每日限量仅约束定时任务：当天已成功采集过则跳过；手动「立即采集」不受限，可随时补充新名言。
        if ("SCHEDULED".equals(fetchType) && quoteFetchLogMapper.countSuccessByFetchDate(today) > 0) {
            LOGGER.info("[DB采集] {} 当日定时采集已完成（SUCCESS），跳过", today);
            return new FetchResultResponse(0, 0, 0, LocalDateTime.now());
        }

        int created = 0;
        int skipped = 0;
        int attempts = 0;
        InternetQuoteException lastError = null;

        // ===== 阶段一：中文优先 =====
        // 1) 先采集在线中文名人名言（hitokoto / 清和 / 客官 + 已配置 key 的源）
        int[] onlineResult = collectOnlineChinese(today, dailyLimit);
        created += onlineResult[0];
        skipped += onlineResult[1];
        LOGGER.info("[DB采集] 阶段一(在线中文名人名言)：新增 {} 条（跳过重复 {} 条），当前累计 {} 条",
                onlineResult[0], onlineResult[1], created);
        // 2) 再导入离线中文精选集补足（幂等，仅新增尚未入库的）；仍不足则交由阶段二。
        ImportResultResponse chinese = chineseQuoteImporter.importBuiltIn(dailyLimit - created);
        created += chinese.inserted();
        skipped += chinese.skipped();
        LOGGER.info("[DB采集] 阶段一(离线中文精选)：新增 {} 条（资源共 {} 条，跳过重复 {} 条），当前累计 {} 条",
                chinese.inserted(), chinese.total(), chinese.skipped(), created);

        // ===== 阶段二：中文不足配额时，采集其他语言并翻译为中文 补充 =====
        List<Quote> pending = new ArrayList<>();
        Set<String> batchFingerprints = new HashSet<>();
        if (created < dailyLimit) {
            int target = dailyLimit - created; // 还需补足的条数（外文经翻译后为中文）
            // 反复拉取直到补足或达到最大尝试次数；重复条目跳过并继续补充。
            while (pending.size() < target && attempts < MAX_ATTEMPTS) {
                attempts++;
                int need = target - pending.size();
                // 按预估重复率多拉一批，提高命中不重复名言的概率，减少空转；
                // 但单次拉取条数不得超过远程接口的安全上限（超出会整页返回空）。
                int pageSize = Math.max(1, Math.min(need * FETCH_PAGE_FACTOR, maxPageSize));
                List<RemoteQuote> remoteQuotes;
                try {
                    remoteQuotes = internetQuoteClient.fetchQuotes(pageSize);
                } catch (InternetQuoteException exception) {
                    LOGGER.warn("[DB采集] 阶段二 第 {} 次拉取失败：{}", attempts, exception.getMessage());
                    lastError = exception;
                    continue;
                }

                for (RemoteQuote remote : remoteQuotes) {
                    if (pending.size() >= target) {
                        break;
                    }
                    String fingerprint = Quote.fingerprintOf(remote.content(), remote.author());
                    if (batchFingerprints.contains(fingerprint) || quoteMapper.existsByFingerprint(fingerprint)) {
                        // 重复：跳过本条，本轮后续及下一轮继续采集以补充配额
                        skipped++;
                        continue;
                    }
                    batchFingerprints.add(fingerprint);
                    Quote quote = new Quote();
                    // 外文采集翻译为中文；翻译失败（网络/配额）时降级保留原文，不阻断采集。
                    String original = remote.content();
                    if (translateEnabled) {
                        String zh = translationService.translateToChinese(original);
                        if (zh != null && !zh.isBlank()) {
                            quote.setContent(zh);
                            quote.setOriginalContent(original);
                        } else {
                            quote.setContent(original);
                        }
                    } else {
                        quote.setContent(original);
                    }
                    quote.setAuthor(remote.author());
                    quote.setSource(remote.source());
                    quote.setPopularity(remote.popularity());
                    quote.setFeatured(false);
                    quote.setOrigin(QuoteOrigin.INTERNET.name());
                    quote.setLang("en");
                    quote.setLastFetchedDate(today);
                    quote.setFingerprint(fingerprint);
                    pending.add(quote);
                }
            }
        }

        // 两个阶段都没有产出且阶段二拉取失败：记录 FAILED 并向上抛出
        if (created == 0 && pending.isEmpty() && lastError != null) {
            writeLog(today, fetchType, 0, skipped, attempts, "FAILED", lastError.getMessage());
            throw lastError;
        }

        // 批量写入阶段二采集（翻译为中文）结果
        if (!pending.isEmpty()) {
            int duplicateOnWrite = 0;
            for (Quote quote : pending) {
                try {
                    quoteMapper.insert(quote);
                } catch (DataIntegrityViolationException exception) {
                    // 极端并发下指纹唯一键冲突：视为重复，跳过并计入跳过数（采集阶段已尽量去重）
                    LOGGER.warn("[DB采集] 写入时发生指纹唯一键冲突，按重复跳过 fingerprint={}", quote.getFingerprint());
                    duplicateOnWrite++;
                }
            }
            int inserted = pending.size() - duplicateOnWrite;
            created += inserted;
            skipped += duplicateOnWrite;
            LOGGER.info("[DB采集] 阶段二(外文翻译为中文)：新增 {} 条，跳过重复 {} 条，尝试 {} 次；本次合计新增 {} 条",
                    inserted, duplicateOnWrite, attempts, created);
        }

        writeLog(today, fetchType, created, skipped, attempts, "SUCCESS", null);
        return new FetchResultResponse(created, skipped, attempts, LocalDateTime.now());
    }

    /**
     * 阶段一补充：采集在线中文名人名言并入库（来源 {@code CN_INTERNET}，语种 zh）。
     *
     * <p>从 {@link ChineseQuoteClient} 多源（hitokoto / 清和 / 客官 + 已配置 key 的源）取回中文名言，
     * 按「内容+作者」指纹去重后写入；本次最多新增 {@code remaining} 条、并受在线限额约束，
     * 以控制单次采集耗时。</p>
     *
     * @return 长度 2 的数组：[{@code [0]}=新增条数, {@code [1]}=跳过重复条数]
     */
    private int[] collectOnlineChinese(LocalDate today, int remaining) {
        if (remaining <= 0 || chineseOnlineLimit <= 0) {
            return new int[]{0, 0};
        }
        List<RemoteQuote> online = chineseQuoteClient.fetchChineseQuotes(Math.min(remaining, chineseOnlineLimit));
        int added = 0;
        int duplicated = 0;
        for (RemoteQuote remote : online) {
            if (added >= remaining) {
                break;
            }
            String content = remote.content() == null ? "" : remote.content().strip();
            String author = (remote.author() == null || remote.author().isBlank()) ? "佚名" : remote.author().strip();
            if (content.isEmpty()) {
                continue;
            }
            String fingerprint = Quote.fingerprintOf(content, author);
            if (quoteMapper.existsByFingerprint(fingerprint)) {
                duplicated++;
                continue;
            }
            Quote quote = new Quote();
            quote.setContent(content);
            quote.setAuthor(author);
            quote.setSource(remote.source() == null || remote.source().isBlank() ? "中文在线名人名言" : remote.source());
            quote.setPopularity(60);
            quote.setFeatured(false);
            quote.setOrigin(QuoteOrigin.CN_INTERNET.name());
            quote.setLang("zh");
            quote.setLastFetchedDate(today);
            quote.setFingerprint(fingerprint);
            quoteMapper.insert(quote);
            added++;
        }
        return new int[]{added, duplicated};
    }

    /**
     * 写入一条采集任务日志，记录业务日期、触发方式、成功/跳过数量、尝试次数与状态。
     *
     * @param date      业务日期
     * @param fetchType 触发方式
     * @param fetched   成功写入条数
     * @param skipped   跳过重复条数
     * @param attempts  采集尝试次数
     * @param status    状态 SUCCESS / FAILED
     * @param error     失败时的错误信息（成功传 null）
     */
    private void writeLog(LocalDate date, String fetchType, int fetched, int skipped,
                          int attempts, String status, String error) {
        QuoteFetchLog log = new QuoteFetchLog();
        log.setFetchDate(date);
        log.setFetchType(fetchType);
        log.setFetchedCount(fetched);
        log.setSkippedCount(skipped);
        log.setTotalAttempts(attempts);
        log.setStatus(status);
        log.setErrorMessage(error);
        log.setCreatedAt(LocalDateTime.now());
        quoteFetchLogMapper.insert(log);
        LOGGER.info("[DB采集] 写入采集日志 id={} status={}", log.getId(), status);
    }

    /**
     * 回填翻译：将历史「外文采集但未翻译」的名言翻译为中文。
     *
     * <p>用于翻译能力上线前、或免费翻译额度耗尽时残留的英文名言——{@code content} 改写为中文、
     * {@code originalContent} 留存英文原文、{@code lang} 标记为 {@code en}。本方法：
     * <ul>
     *   <li><b>逐条跳过</b>：单条翻译失败（配额/网络）不中断整批，跳过继续下一条；</li>
     *   <li><b>小间隔节流</b>：每条之间 sleep 毫秒级间隔（{@code translate-interval-ms}），
     *       避免瞬时突发浪费免费额度；</li>
     *   <li><b>可反复续做</b>：成功翻译的记录 {@code original_content} 非空，下次触发自动跳过；
     *       连续失败达到阈值（判定额度耗尽）即结束本轮，剩余条目可再次调用本接口续做。</li>
     * </ul>
     * 不开启事务，逐条独立提交，便于中断后继续。</p>
     *
     * @return 本轮回填结果（成功数、失败数、剩余未翻译数）
     */
    public TranslateResultResponse translatePendingQuotes() {
        List<Quote> pending = quoteMapper.selectList(
                new QueryWrapper<Quote>().eq("origin", QuoteOrigin.INTERNET.name()).isNull("original_content"));
        if (pending.isEmpty()) {
            LOGGER.info("[翻译回填] 无待翻译的外文名言");
            return new TranslateResultResponse(0, 0, 0);
        }
        LOGGER.info("[翻译回填] 发现 {} 条待翻译外文名言，开始逐条翻译（间隔 {}ms，连续失败上限 {}）",
                pending.size(), translateIntervalMs, maxConsecutiveFailures);
        int translated = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        for (Quote quote : pending) {
            if (consecutiveFailures >= maxConsecutiveFailures) {
                LOGGER.warn("[翻译回填] 连续 {} 条翻译失败，判定免费额度已耗尽，结束本轮（可稍后再次触发续做）",
                        consecutiveFailures);
                break;
            }
            String zh = translationService.translateToChinese(quote.getContent());
            if (zh == null || zh.isBlank()) {
                failed++;
                consecutiveFailures++;
                sleep(translateIntervalMs);
                continue;
            }
            quote.setOriginalContent(quote.getContent());
            quote.setContent(zh);
            quote.setLang("en");
            quoteMapper.updateById(quote);
            translated++;
            consecutiveFailures = 0;
            sleep(translateIntervalMs);
        }
        int remaining = pending.size() - translated;
        LOGGER.info("[翻译回填] 本轮完成：成功 {} 条，失败 {} 条，剩余待翻译 {} 条（可再次触发续做）",
                translated, failed, remaining);
        return new TranslateResultResponse(translated, failed, remaining);
    }

    /** 节流等待（忽略中断）。 */
    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
