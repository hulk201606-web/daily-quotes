package com.example.dailyquotes.exception;

/**
 * 未登录/未识别用户异常：当接口要求携带用户身份（如个人标签、收藏）但请求未提供有效用户时抛出，
 * 由 {@link GlobalExceptionHandler} 转换为 401 响应。
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
