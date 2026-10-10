package com.example.dailyquotes.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一错误响应结构。
 *
 * <p>所有经 {@link GlobalExceptionHandler} 捕获的异常都会转换为该对象返回，
 * 包含时间戳、HTTP 状态码、可读错误信息以及字段级校验错误映射（如有）。</p>
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors
) {
    public ApiError(int status, String message) {
        this(LocalDateTime.now(), status, message, Map.of());
    }
}
