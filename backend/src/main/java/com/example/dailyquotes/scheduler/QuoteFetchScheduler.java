package com.example.dailyquotes.scheduler;

import com.example.dailyquotes.dto.FetchResultResponse;
import com.example.dailyquotes.service.QuoteFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日名言采集调度器。
 *
 * <p>依据配置 {@code app.quote-fetch.cron}（默认每天 06:30，时区 {@code app.quote-fetch.zone}）定时触发，
 * 调用采集服务完成一次「每日 10 条」的互联网名言拉取。任务以 SCHEDULED 类型执行；
 * 单点失败仅记录日志，等待下一次调度重试，不影响主流程。</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.quote-fetch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QuoteFetchScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteFetchScheduler.class);

    private final QuoteFetchService quoteFetchService;

    public QuoteFetchScheduler(QuoteFetchService quoteFetchService) {
        this.quoteFetchService = quoteFetchService;
    }

    /** 定时回调：触发一次每日采集，并汇总结果日志；异常仅记录、不影响后续调度。 */
    @Scheduled(cron = "${app.quote-fetch.cron}", zone = "${app.quote-fetch.zone}")
    public void fetchDailyQuotes() {
        try {
            FetchResultResponse result = quoteFetchService.fetchNow("SCHEDULED");
            LOGGER.info("每日名言同步完成：新增 {} 条，跳过 {} 条，尝试 {} 次",
                    result.created(), result.skipped(), result.attempts());
        } catch (Exception exception) {
            LOGGER.error("每日名言同步失败，将在下一次计划任务重试", exception);
        }
    }
}
