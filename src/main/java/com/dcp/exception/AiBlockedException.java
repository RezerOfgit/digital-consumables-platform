package com.dcp.exception;

/**
 * AI 配伍禁忌预警专属异常
 * @author Re-zero
 * @version 1.0
 */
public class AiBlockedException extends RuntimeException {

    public AiBlockedException(String message) {
        super(message);
    }
}
