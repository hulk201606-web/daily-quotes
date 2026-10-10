package com.example.dailyquotes.dto;

/**
 * 外文名言回填翻译结果响应。
 *
 * <p>用于「翻译回填」接口的返回，暴露本轮成功翻译条数、失败（跳过）条数与剩余待翻译条数。
 * 剩余条目可通过再次触发该接口续做（成功翻译的记录已带原文，不会被重复处理）。</p>
 */
public record TranslateResultResponse(int translated, int failed, int remaining) {
}
