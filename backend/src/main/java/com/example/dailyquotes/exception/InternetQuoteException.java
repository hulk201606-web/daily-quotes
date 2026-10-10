package com.example.dailyquotes.exception;

/**
 * 互联网名言服务异常：当远程拉取失败（网络/解析错误）时由采集客户端抛出，
 * 由 {@link GlobalExceptionHandler} 转换为 502 响应。
 */
public class InternetQuoteException extends RuntimeException {

    public InternetQuoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
