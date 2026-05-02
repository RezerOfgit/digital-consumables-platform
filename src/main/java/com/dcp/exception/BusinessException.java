package com.dcp.exception;

import lombok.Getter;

/**
 * 自定义通用业务异常
 * @author Re-zero
 * @version 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
