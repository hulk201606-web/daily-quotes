package com.example.dailyquotes.exception;

/**
 * 名言重复异常：当新增/编辑的名言（内容+作者）已存在时由服务层抛出，
 * 由 {@link GlobalExceptionHandler} 转换为 409 冲突响应。
 */
public class DuplicateQuoteException extends RuntimeException {

    public DuplicateQuoteException(String message) {
        super(message);
    }
}
