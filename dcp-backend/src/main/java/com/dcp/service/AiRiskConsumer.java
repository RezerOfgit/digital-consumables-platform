package com.dcp.service;

import com.dcp.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * AI 风控消费者，监听 MQ 队列，调用 AiRiskService 执行风控逻辑。
 * 配合 Spring Retry，消费失败自动重试 3 次。
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Service
public class AiRiskConsumer {

    @Resource
    private AiRiskService aiRiskService;

    /**
     * 消费单品风控消息。
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SINGLE)
    public void handleSingleRisk(Map<String, Object> message) {
        Long recordId = ((Number) message.get("recordId")).longValue();
        String applicant = (String) message.get("applicant");
        String materialName = (String) message.get("materialName");
        Integer quantity = ((Number) message.get("quantity")).intValue();
        String remark = (String) message.get("remark");

        log.info("[MQ 消费] 单品风控开始: recordId={}, 耗材={}", recordId, materialName);
        aiRiskService.executeSingleRiskCheck(recordId, applicant, materialName, quantity, remark);
    }

    /**
     * 消费批量风控消息。
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_BATCH)
    public void handleBatchRisk(Map<String, Object> message) {
        List<Number> recordIdNumbers = (List<Number>) message.get("recordIds");
        List<Long> recordIds = recordIdNumbers.stream()
                .map(Number::longValue)
                .toList();
        String applicant = (String) message.get("applicant");
        String remark = (String) message.get("remark");
        String aiItemListStr = (String) message.get("aiItemListStr");

        log.info("[MQ 消费] 批量风控开始: 记录数={}", recordIds.size());
        aiRiskService.executeBatchRiskCheck(recordIds, applicant, remark, aiItemListStr);
    }
}