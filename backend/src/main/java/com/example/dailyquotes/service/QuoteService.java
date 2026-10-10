package com.example.dailyquotes.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dailyquotes.domain.Quote;
import com.example.dailyquotes.domain.QuoteOrigin;
import com.example.dailyquotes.domain.User;
import com.example.dailyquotes.domain.UserFavorite;
import com.example.dailyquotes.domain.UserQuoteTag;
import com.example.dailyquotes.domain.UserTag;
import com.example.dailyquotes.dto.PageResponse;
import com.example.dailyquotes.dto.QuoteRequest;
import com.example.dailyquotes.dto.QuoteResponse;
import com.example.dailyquotes.dto.QuoteStatsResponse;
import com.example.dailyquotes.exception.DuplicateQuoteException;
import com.example.dailyquotes.exception.ResourceNotFoundException;
import com.example.dailyquotes.mapper.QuoteMapper;
import com.example.dailyquotes.mapper.UserFavoriteMapper;
import com.example.dailyquotes.mapper.UserQuoteTagMapper;
import com.example.dailyquotes.mapper.UserTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 名言业务服务：负责名言的查询、新增、编辑、删除与统计。
 *
 * <p>所有写操作在写入前都会通过指纹（内容+作者）进行去重校验，保证同一名言不会被重复录入；
 * 全部数据库读写均记录统一前缀的业务日志（[DB查询]/[DB新增]/[DB更新]/[DB删除]/[DB统计]），
 * 便于问题排查与数据变更审计。</p>
 */
@Service
public class QuoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteService.class);

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "popularity", "popularity",
            "source", "source",
            "author", "author",
            "latest", "created_at",
            "fetched", "last_fetched_date"
    );

    private final QuoteMapper quoteMapper;
    private final UserTagMapper userTagMapper;
    private final UserQuoteTagMapper userQuoteTagMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final int dailyLimit;

    public QuoteService(QuoteMapper quoteMapper,
                        UserTagMapper userTagMapper,
                        UserQuoteTagMapper userQuoteTagMapper,
                        UserFavoriteMapper userFavoriteMapper,
                        @org.springframework.beans.factory.annotation.Value("${app.quote-fetch.daily-limit:30}") int dailyLimit) {
        this.quoteMapper = quoteMapper;
        this.userTagMapper = userTagMapper;
        this.userQuoteTagMapper = userQuoteTagMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.dailyLimit = dailyLimit;
    }

    /**
     * 分页查询名言。
     *
     * <p>支持按关键词（模糊匹配内容或作者）、按出处精确筛选，并可按
     * 知名度 / 出处 / 作者 / 录入时间排序；最终以主键倒序作为稳定排序兜底，
     * 避免同分值记录分页时出现抖动。页码与每页大小做了合法性兜底（页码≥0、每页 1-100）。</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> findQuotes(String keyword,
                                                  String source,
                                                  String lang,
                                                  String sortBy,
                                                  String direction,
                                                  int page,
                                                  int size,
                                                  User user) {
        LOGGER.info("[DB查询] 分页查询名言 keyword={}, source={}, lang={}, sortBy={}, direction={}, page={}, size={}",
                keyword, source, lang, sortBy, direction, page, size);
        QueryWrapper<Quote> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("content", keyword).or().like("author", keyword);
        }
        if (StringUtils.hasText(source)) {
            wrapper.eq("source", source);
        }
        if (StringUtils.hasText(lang)) {
            wrapper.eq("lang", lang);
        }
        String property = SORT_FIELDS.getOrDefault(sortBy, "popularity");
        boolean asc = "asc".equalsIgnoreCase(direction);
        if ("fetched".equals(sortBy)) {
            // 「按采集时间」：采集日期新→旧（可切换）；同一采集日内「中文来源置顶」
            // （对齐「中文优先」采集策略，让中文 API 直采内容在该天顶部可见），
            // 同组内再按录入时间新→旧（last_fetched_date 仅 DATE 粒度，需 created_at 兜底到具体时间）
            if (asc) {
                wrapper.orderByAsc("last_fetched_date");
            } else {
                wrapper.orderByDesc("last_fetched_date");
            }
            wrapper.orderByAsc("CASE WHEN origin IN ('CN_INTERNET','CN_SEED','SEED') THEN 0 ELSE 1 END");
            if (asc) {
                wrapper.orderByAsc("created_at");
            } else {
                wrapper.orderByDesc("created_at");
            }
        } else if (asc) {
            wrapper.orderByAsc(property);
        } else {
            wrapper.orderByDesc(property);
        }
        wrapper.orderByDesc("id");

        // 注意：请求 page 为 0 基，而 MyBatis-Plus Page.current 为 1 基，需 +1 转换，
        // 否则 page=0 与 page=1 都会返回第一页（前端「前两页内容相同」的根因）
        IPage<Quote> result = quoteMapper.selectPage(
                new Page<>(Math.max(page, 0) + 1, Math.min(Math.max(size, 1), 100)), wrapper);
        List<QuoteResponse> items = toResponses(result.getRecords(), user);
        LOGGER.info("[DB查询] 返回名言 {} 条，总计 {} 条", items.size(), result.getTotal());
        return PageResponse.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 按主键查询单条名言，不存在时抛出资源未找到异常；若携带用户则附带其个人标签与收藏标记。 */
    @Transactional(readOnly = true)
    public QuoteResponse findById(Long id, User user) {
        LOGGER.info("[DB查询] 按主键查询名言 id={}", id);
        return toResponses(List.of(getQuote(id)), user).get(0);
    }

    /**
     * 人工维护：新增一条名言。
     *
     * <p>写入前先做去重校验（excludeId 为 null 表示新增），通过后生成指纹并标记来源为 MANUAL，
     * 再执行插入；创建时间、更新时间由自动填充处理器维护。</p>
     */
    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        LOGGER.info("[DB新增] 准备写入名言 author={}, source={}, popularity={}", request.author(), request.source(), request.popularity());
        assertNotDuplicate(request.content(), request.author(), null);
        Quote quote = new Quote();
        applyRequest(quote, request);
        quote.setOrigin(QuoteOrigin.MANUAL.name());
        quote.setLang("zh");
        int rows = quoteMapper.insert(quote);
        LOGGER.info("[DB新增] 名言已写入 id={}, 影响行数={}", quote.getId(), rows);
        return QuoteResponse.from(quote);
    }

    /**
     * 人工维护：编辑指定名言。
     *
     * <p>先加载已有记录（不存在则抛异常），再基于传入内容做去重校验并排除自身（excludeId=当前 id），
     * 校验通过后覆盖内容、作者、出处、知名度、精选状态与指纹，执行按主键更新。</p>
     */
    @Transactional
    public QuoteResponse update(Long id, QuoteRequest request) {
        LOGGER.info("[DB更新] 准备更新名言 id={}, author={}, source={}, popularity={}", id, request.author(), request.source(), request.popularity());
        Quote quote = getQuote(id);
        assertNotDuplicate(request.content(), request.author(), id);
        applyRequest(quote, request);
        int rows = quoteMapper.updateById(quote);
        LOGGER.info("[DB更新] 名言已更新 id={}, 影响行数={}", id, rows);
        return QuoteResponse.from(quote);
    }

    /** 删除指定名言：先确认记录存在，再按主键物理删除。 */
    @Transactional
    public void delete(Long id) {
        LOGGER.info("[DB删除] 准备删除名言 id={}", id);
        Quote quote = getQuote(id);
        int rows = quoteMapper.deleteById(quote.getId());
        LOGGER.info("[DB删除] 名言已删除 id={}, 影响行数={}", id, rows);
    }

    /**
     * 汇总展示所需的统计指标。
     *
     * <p>返回名言总数、互联网采集数、人工维护数、今日互联网采集数、去重出处数量与出处列表，
     * 供前端概览面板与筛选下拉使用。各计数通过 Mapper 自定义聚合查询获得。</p>
     */
    @Transactional(readOnly = true)
    public QuoteStatsResponse stats() {
        LOGGER.info("[DB统计] 汇总名言总量与出处信息");
        List<String> sources = quoteMapper.selectDistinctSources();
        long total = Long.valueOf(quoteMapper.selectCount(new QueryWrapper<>()));
        long fetched = Long.valueOf(quoteMapper.countByOrigin(QuoteOrigin.INTERNET.name()));
        long manual = Long.valueOf(quoteMapper.countByOrigin(QuoteOrigin.MANUAL.name()));
        long chineseSeed = Long.valueOf(quoteMapper.countByOrigin(QuoteOrigin.CN_SEED.name()));
        long fetchedToday = Long.valueOf(quoteMapper.countByOriginAndFetchedDate(
                QuoteOrigin.INTERNET.name(), LocalDate.now()));
        QuoteStatsResponse response = new QuoteStatsResponse(
                total, fetched, manual, chineseSeed, fetchedToday, dailyLimit, (long) sources.size(), sources);
        LOGGER.info("[DB统计] 总计 {} 条，今日采集 {} 条，出处 {} 个", total, fetchedToday, sources.size());
        return response;
    }

    /** 将请求体字段映射到实体，并重算去重指纹（不含来源与日志字段）。 */
    private void applyRequest(Quote quote, QuoteRequest request) {
        quote.setContent(request.content());
        quote.setAuthor(request.author());
        quote.setSource(request.source());
        quote.setPopularity(request.popularity());
        quote.setFeatured(request.featured());
        quote.setFingerprint(Quote.fingerprintOf(request.content(), request.author()));
    }

    /**
     * 去重校验：若内容+作者组合已存在且不是被排除的自身记录，则抛出重复异常。
     *
     * @param content    内容
     * @param author     作者
     * @param excludeId 编辑场景下的当前记录 id（可为 null 表示新增）
     */
    private void assertNotDuplicate(String content, String author, Long excludeId) {
        String fingerprint = Quote.fingerprintOf(content, author);
        if (!quoteMapper.existsByFingerprint(fingerprint)) {
            return;
        }
        Quote existing = quoteMapper.selectOne(
                new QueryWrapper<Quote>().eq("fingerprint", fingerprint).last("LIMIT 1"));
        if (existing != null && excludeId != null && existing.getId().equals(excludeId)) {
            return;
        }
        throw new DuplicateQuoteException("该名言（内容 + 作者）已存在，请勿重复添加");
    }

    /** 按主键查询并断言存在，不存在时抛出资源未找到异常。 */
    private Quote getQuote(Long id) {
        Quote quote = quoteMapper.selectById(id);
        if (quote == null) {
            throw new ResourceNotFoundException("未找到 id=" + id + " 的名言");
        }
        return quote;
    }

    /**
     * 将名言实体集合转换为对外响应，并按需注入当前用户的个人视角信息。
     *
     * <p>当 {@code user} 为 {@code null}（匿名访问）或集合为空时，仅做基础转换；
     * 否则一次性加载该用户的「标签名映射 / 名言-标签关联 / 收藏集合」，
     * 为每条名言补充 {@code myTags}（个人标签名）与 {@code favorited}（是否已收藏），
     * 避免逐条查询带来的 N+1 性能问题。</p>
     *
     * @param quotes 名言实体集合
     * @param user   当前用户（可为 null）
     * @return 带有个人化信息的响应对象集合
     */
    public List<QuoteResponse> toResponses(List<Quote> quotes, User user) {
        if (user == null || quotes.isEmpty()) {
            return quotes.stream().map(QuoteResponse::from).collect(Collectors.toList());
        }

        // 1) 该用户全部个人标签：id -> 名称
        Map<Long, String> tagNameById = userTagMapper
                .selectList(new QueryWrapper<UserTag>().eq("user_id", user.getId()))
                .stream().collect(Collectors.toMap(UserTag::getId, UserTag::getName, (a, b) -> a));

        List<Long> quoteIds = quotes.stream().map(Quote::getId).collect(Collectors.toList());

        // 2) 该用户在这些名言上的标签关联：quoteId -> 标签名列表
        Map<Long, List<String>> tagsByQuote = userQuoteTagMapper
                .selectList(new QueryWrapper<UserQuoteTag>().eq("user_id", user.getId()).in("quote_id", quoteIds))
                .stream()
                .collect(Collectors.groupingBy(
                        UserQuoteTag::getQuoteId,
                        Collectors.mapping(rel -> tagNameById.getOrDefault(rel.getTagId(), null),
                                Collectors.filtering(java.util.Objects::nonNull, Collectors.toList()))));

        // 3) 该用户已收藏的名言 id 集合
        Set<Long> favoritedIds = userFavoriteMapper
                .selectList(new QueryWrapper<UserFavorite>().eq("user_id", user.getId()).in("quote_id", quoteIds))
                .stream().map(UserFavorite::getQuoteId).collect(Collectors.toSet());

        List<QuoteResponse> result = new ArrayList<>(quotes.size());
        for (Quote quote : quotes) {
            result.add(QuoteResponse.from(
                    quote,
                    tagsByQuote.getOrDefault(quote.getId(), List.of()),
                    favoritedIds.contains(quote.getId())));
        }
        return result;
    }
}
