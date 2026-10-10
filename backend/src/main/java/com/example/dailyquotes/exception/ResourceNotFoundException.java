package com.example.dailyquotes.exception;

/**
 * 资源未找到异常：当按 id 查询的名言不存在时由服务层抛出，
 * 由 {@link GlobalExceptionHandler} 转换为 404 响应。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
