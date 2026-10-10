package com.example.dailyquotes.client;

/**
 * 互联网名言的传输对象（DTO）。
 *
 * <p>仅承载采集阶段所需的字段，未包含来源标签与持久化元数据；
 * 默认知名度取 50，来源统一标记为「互联网公开名言库」。</p>
 */
public record RemoteQuote(String content, String author, String source, Integer popularity) {

    public RemoteQuote(String content, String author, String source) {
        this(content, author, source, 50);
    }
}
