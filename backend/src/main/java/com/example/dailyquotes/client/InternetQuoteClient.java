package com.example.dailyquotes.client;

import com.example.dailyquotes.exception.InternetQuoteException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 互联网名言客户端。
 *
 * <p>通过配置项 {@code app.quote-fetch.url}（含 {@code {limit}} 占位符）调用公开名言接口，
 * 将响应解析为 {@link RemoteQuote} 列表。解析时兼容「外层包裹 quotes 数组」或直接返回数组/对象的多种结构，
 * 并只保留内容、作者均非空的有效条目；任意网络异常都会被包装为 {@link InternetQuoteException} 向上抛出。</p>
 */
@Component
public class InternetQuoteClient {

    private final RestTemplate restTemplate;
    private final String endpointTemplate;

    public InternetQuoteClient(RestTemplateBuilder builder,
                               @Value("${app.quote-fetch.url}") String endpointTemplate) {
        this.restTemplate = builder.build();
        this.endpointTemplate = endpointTemplate;
    }

    /** 拉取最多 {@code limit} 条远程名言，并对网络异常做统一包装。 */
    public List<RemoteQuote> fetchQuotes(int limit) {
        String endpoint = endpointTemplate.replace("{limit}", String.valueOf(limit));
        try {
            JsonNode response = restTemplate.getForObject(endpoint, JsonNode.class);
            return parse(response);
        } catch (Exception exception) {
            throw new InternetQuoteException("无法连接互联网名言服务，请稍后重试", exception);
        }
    }

    /** 解析远程响应：兼容 {quotes:[...]}、数组、单对象三种形态，返回有效名言列表。 */
    private List<RemoteQuote> parse(JsonNode response) {
        if (response == null) {
            return List.of();
        }

        JsonNode items = response;
        if (response.isObject() && response.has("quotes")) {
            items = response.get("quotes");
        }

        List<RemoteQuote> quotes = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(item -> addIfValid(quotes, item));
        } else if (items.isObject()) {
            addIfValid(quotes, items);
        }
        return quotes;
    }

    /** 将单个 JSON 节点转为 RemoteQuote：仅在内容与作者均非空时加入结果集。 */
    private void addIfValid(List<RemoteQuote> quotes, JsonNode item) {
        String content = textValue(item, "quote", "content");
        String author = textValue(item, "author");
        if (StringUtils.hasText(content) && StringUtils.hasText(author)) {
            quotes.add(new RemoteQuote(content.strip(), author.strip(), "互联网公开名言库"));
        }
    }

    /** 按多个候选字段名依次取第一个文本值，均未命中则返回空串，兼容不同接口的字段命名。 */
    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return "";
    }
}
