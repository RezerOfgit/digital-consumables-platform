package com.dcp.exception;

import com.dcp.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException; // 别忘了导包

/**
 * @author Re-zero
 * @version 1.0
 * 全局异常处理器
 * 作用：拦截系统抛出的所有异常，包装成统一的 JSON 格式返回给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AiBlockedException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleAiBlocked(AiBlockedException e) {
        log.warn("AI预警拦截: {}", e.getMessage());
        return R.fail(4030, e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return R.fail(400, msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return R.fail(400, msg);
    }

    // HTTP 请求方法错误（GET 访问 POST 接口等）
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不允许: {}", e.getMessage());
        return R.fail(405, "请求方法不允许，请使用 " + String.join(", ", e.getSupportedMethods()));
    }

    // ==================== 权限安全异常 ====================

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)   // HTTP 403
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return R.fail(403, "抱歉，您的角色权限不足，拒绝访问！");  // 业务码也建议用 403
    }

    // ==================== 系统兜底 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统内部错误");
    }
}
