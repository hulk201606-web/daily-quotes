package com.example.dailyquotes.exception;

/**
 * 权限不足异常：当当前用户无权执行某操作（如非管理员尝试修改/删除名言）时由权限守卫抛出，
 * 由 {@link GlobalExceptionHandler} 转换为 403 响应。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
