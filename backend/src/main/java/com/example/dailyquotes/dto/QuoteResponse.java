package com.example.dailyquotes.dto;

import com.example.dailyquotes.domain.Quote;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 名言对外响应对象。
 *
 * <p>由 {@link #from(Quote)} 从实体转换而来，对外暴露 id、内容、作者、出处、知名度、是否精选、
 * 来源标签、采集日期与时间戳；{@code featured} 以基本类型布尔输出，避免空值歧义。</p>
 *
 * <p>当请求携带了当前用户（{@code X-User-Id}）时，可额外附带该用户的个人视角信息：
 * {@code myTags}（当前用户给本条名言打的标签名列表）与 {@code favorited}（当前用户是否已收藏），
 * 便于前端区分「公共数据」与「我的收藏/标签」。两者均为 nullable，匿名访问时不返回。</p>
 */
public record QuoteResponse(
        Long id,
        String content,
        String author,
        String source,
        Integer popularity,
        boolean featured,
        String origin,
        LocalDate lastFetchedDate,
        String lang,
        String originalContent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> myTags,
        Boolean favorited
) {
    /** 将持久化实体转换为对外响应对象（无个人视角信息）。 */
    public static QuoteResponse from(Quote quote) {
        return from(quote, null, null);
    }

    /** 将持久化实体转换为对外响应对象，并附带当前用户的个人标签与收藏标记。 */
    public static QuoteResponse from(Quote quote, List<String> myTags, Boolean favorited) {
        return new QuoteResponse(
                quote.getId(),
                quote.getContent(),
                quote.getAuthor(),
                quote.getSource(),
                quote.getPopularity(),
                Boolean.TRUE.equals(quote.getFeatured()),
                quote.getOrigin(),
                quote.getLastFetchedDate(),
                quote.getLang(),
                quote.getOriginalContent(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                myTags,
                favorited
        );
    }
}
