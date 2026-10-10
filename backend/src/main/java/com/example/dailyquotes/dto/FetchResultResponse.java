package com.example.dailyquotes.dto;

import java.time.LocalDateTime;

/**
 * 手动/定时采集的结果摘要。
 *
 * <p>返回本次新增写入条数、因重复跳过条数、采集尝试次数与完成时间，
 * 供前端立即采集按钮的反馈与调度器日志记录使用。</p>
 */
public record FetchResultResponse(
        int created,
        int skipped,
        int attempts,
        LocalDateTime completedAt
) {
}
