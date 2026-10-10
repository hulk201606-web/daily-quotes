package com.example.dailyquotes.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 免费匿名翻译服务（MyMemory）。
 *
 * <p>将外文（默认英文）名言翻译成中文，用于「采集外文后做中文对应翻译」的需求。
 * 采用 MyMemory 的公开接口，无需任何密钥，但存在每日字数/次数配额限制
 * （匿名约 5000 字符/500 次每天）。接口在配额耗尽或网络异常时会返回失败标记，
 * 此时本服务直接返回 {@code null}，由调用方降级为「保留原文、不翻译」，保证采集流程不被中断。</p>
 */
@Component
public class TranslationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationService.class);

    private final RestTemplate restTemplate;
    private final String endpoint;
    private final String sourceLang;
    private final String targetLang;

    public TranslationService(RestTemplateBuilder builder,
                              @Value("${app.translation.url:https://api.mymemory.translated.net/get}") String endpoint,
                              @Value("${app.translation.source-lang:en}") String sourceLang,
                              @Value("${app.translation.target-lang:zh-CN}") String targetLang) {
        this.restTemplate = builder.build();
        this.endpoint = endpoint;
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
    }

    /**
     * 将文本翻译为中文。
     *
     * @param text 待翻译文本（为空或空白直接返回 null）
     * @return 翻译后的中文；任何失败（网络异常、配额耗尽、无效结果）均返回 null
     */
    public String translateToChinese(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(endpoint)
                    .queryParam("q", text)
                    .queryParam("langpair", sourceLang + "|" + targetLang)
                    .build().toUri();
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            if (response == null) {
                return null;
            }
            // 配额耗尽：quotaFinished=true
            if (response.has("quotaFinished") && response.get("quotaFinished").asBoolean(false)) {
                LOGGER.warn("[翻译] MyMemory 当日配额已耗尽，停止翻译");
                return null;
            }
            // 非成功状态（如 4xx）视为失败
            int status = response.path("responseStatus").asInt(200);
            if (status != 200) {
                LOGGER.warn("[翻译] MyMemory 返回状态 {}，原文保留", status);
                return null;
            }
            JsonNode translated = response.path("responseData").path("translatedText");
            if (translated.isMissingNode() || !translated.isTextual()) {
                return null;
            }
            String result = translated.asText();
            // 接口在异常时也会返回 200，但 translatedText 携带错误文案，需识别并降级
            if (result.isBlank()
                    || result.contains("MYMEMORY WARNING")
                    || result.contains("INVALID LANGUAGE PAIR")
                    || result.contains("QUOTA")) {
                LOGGER.warn("[翻译] MyMemory 返回无效结果（可能为配额/限流/参数错误），原文保留");
                return null;
            }
            return result;
        } catch (Exception exception) {
            LOGGER.warn("[翻译] 调用翻译服务失败：{}", exception.getMessage());
            return null;
        }
    }
}
