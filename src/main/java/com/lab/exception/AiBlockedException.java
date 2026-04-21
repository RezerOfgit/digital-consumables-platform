package com.lab.exception;

/**
 * @author Re-zero
 * @version 1.0
 * AI 配伍禁忌预警专属异常
 */
public class AiBlockedException extends RuntimeException {

    public AiBlockedException(String message) {
        // 直接调用父类 RuntimeException 的构造方法
        super(message);
    }
}
