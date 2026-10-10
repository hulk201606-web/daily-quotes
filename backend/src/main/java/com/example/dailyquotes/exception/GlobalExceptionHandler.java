package com.example.dailyquotes.exception;

import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理。
 *
 * <p>集中拦截各层抛出的异常并转换为结构化 {@link ApiError} 响应：
 * 资源未找到→404，重复/唯一键冲突→409，互联网采集失败→502，参数校验失败→400，
 * 其余未预期异常→500。从而让前端获得一致的错误格式与明确的业务提示。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 资源未找到（如指定 id 的名言不存在）→ 404。 */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** 业务重复（同名名言）或数据库唯一键冲突 → 409，并给出友好提示。 */
    @ExceptionHandler({DuplicateQuoteException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> handleConflict(Exception exception) {
        String message = exception instanceof DuplicateQuoteException
                ? exception.getMessage()
                : "这条名言已存在，请勿重复添加";
        return response(HttpStatus.CONFLICT, message);
    }

    /** 互联网名言服务不可用 → 502，提示稍后重试。 */
    @ExceptionHandler(InternetQuoteException.class)
    ResponseEntity<ApiError> handleInternetQuote(InternetQuoteException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    /** 请求体校验失败 → 400，并携带逐字段的错误信息便于前端高亮提示。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "请检查填写内容",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    /** 方法参数级约束违反（如路径/查询参数校验）→ 400。 */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, "请求参数不正确");
    }

    /** 未携带有效用户身份（如访问个人标签/收藏）→ 401，提示先登录。 */
    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    /** 权限不足（如非管理员尝试修改/删除公共名言）→ 403，给出明确拒绝提示。 */
    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> handleForbidden(ForbiddenException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    /** 兜底处理其余未预期异常 → 500，避免敏感堆栈外泄，仅返回通用提示；同时记录堆栈便于排查。 */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        LOGGER.error("未预期的服务器异常：", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}
