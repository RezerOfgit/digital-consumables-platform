package com.dcp.exception;

/**
 * AI 配伍禁忌预警异常，触发时自动驳回申请并退还库存。
 * @author Re-zero
 * @version 1.0
 */
public class AiBlockedException extends RuntimeException {

    public AiBlockedException(String message) {
        super(message);
    }
}
