package com.example.dailyquotes.dto;

import java.util.List;

/**
 * 通用分页响应封装。
 *
 * <p>承载当前页数据列表与分页元信息（页码、每页大小、总记录数、总页数、是否首页/末页）。
 * 通过 {@link #of(List, long, long, long)} 由 MyBatis-Plus 的 {@code IPage} 结果转换得到，
 * 总页数按向上取整计算，首页/末页标志依据当前页码与总页数判定。</p>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /** 由数据列表与分页元信息构建分页响应，自动计算总页数与首页/末页标志。 */
    public static <T> PageResponse<T> of(List<T> content, long total, long current, long size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / (double) size) : 0;
        boolean first = current <= 1;
        boolean last = current >= totalPages;
        return new PageResponse<>(content, (int) current, (int) size, total, totalPages, first, last);
    }
}
