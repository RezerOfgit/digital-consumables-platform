package com.lab.exception;

import lombok.Getter;

/**
 * @author Re-zero
 * @version 1.0
 * 自定义通用业务异常
 */
@Getter // Lombok注解，自动生成 getCode() 和 getMessage()
public class BusinessException extends RuntimeException {

    private final int code;

    // 构造方法 1：只传错误信息（默认状态码 500）
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    // 构造方法 2：传错误信息 + 自定义状态码
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
