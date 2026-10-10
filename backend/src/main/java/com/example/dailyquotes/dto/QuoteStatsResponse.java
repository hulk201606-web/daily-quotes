package com.example.dailyquotes.dto;

import java.util.List;

/**
 * 名言统计概览响应。
 *
 * <p>聚合展示名言总数、互联网采集数、人工维护数、今日互联网采集数、去重出处数量与出处列表，
 * 支撑前端概览面板与出处筛选下拉。</p>
 */
public record QuoteStatsResponse(
        long total,
        long internetTotal,
        long manualTotal,
        long chineseSeedTotal,
        long fetchedToday,
        long dailyLimit,
        long sourceCount,
        List<String> sources
) {
}
