package com.dcp.service;

import com.dcp.config.RabbitMQConfig;
import com.dcp.dto.ApproveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风控服务。
 * 职责一：发送 MQ 消息（由 RecordService 调用）
 * 职责二：执行风控逻辑（由 AiRiskConsumer 调用）
 * 提示词模板外置在 classpath:templates/ai_risk_prompt.txt，支持无代码调整规则。
 * @author Re-zero
 * @version 2.0
 */
@Slf4j
@Service
public class AiRiskService {

    @Resource
    private RestTemplate restTemplate;

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.model}")
    private String modelName;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    @Resource
    @Lazy
    private RecordService recordService;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private RabbitTemplate rabbitTemplate;

    // ==================== 发送 MQ 消息（主流程调用）====================

    /**
     * 单品风控：发送消息到 MQ。
     */
    public void analyzeRequisitionRisk(Long recordId, String applicant, String materialName, Integer quantity, String remark) {
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过 AI 风控评估");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("recordId", recordId);
        message.put("applicant", applicant);
        message.put("materialName", materialName);
        message.put("quantity", quantity);
        message.put("remark", remark);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_SINGLE,
                message
        );
        log.info("[MQ 发送] 单品风控消息已入队: recordId={}, 耗材={}", recordId, materialName);
    }

    /**
     * 批量风控：发送消息到 MQ。
     */
    public void analyzeBatchRisk(List<Long> recordIds, String applicant, String remark, String aiItemListStr) {
        Map<String, Object> message = new HashMap<>();
        message.put("recordIds", recordIds);
        message.put("applicant", applicant);
        message.put("remark", remark);
        message.put("aiItemListStr", aiItemListStr);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_BATCH,
                message
        );
        log.info("[MQ 发送] 批量风控消息已入队: 记录数={}", recordIds.size());
    }

    // ==================== 执行风控逻辑（Consumer 调用）====================

    /**
     * 执行单品风控评估，调用 DeepSeek 并决定是否标记高危。
     */
    public void executeSingleRiskCheck(Long recordId, String applicant, String materialName, Integer quantity, String remark) {
        try {
            String template = loadPromptTemplate();
            String singleItemList = String.format("- %s (数量: %d)", materialName, quantity);
            String prompt = String.format(template, applicant, remark, singleItemList);

            String aiAdvice = callDeepSeek(prompt);
            log.info("[AI 评估完成] 专家建议：\n{}", aiAdvice);

            if (aiAdvice.contains("高危")) {
                markAsAiHighRisk(List.of(recordId), aiAdvice);
            }
        } catch (Exception e) {
            log.error("[AI 评估失败] 网络异常或 Key 错误：{}", e.getMessage());
            throw e;  // 抛出异常让 Spring Retry 重试
        }
    }

    /**
     * 执行批量风控评估。
     */
    public void executeBatchRiskCheck(List<Long> recordIds, String applicant, String remark, String aiItemListStr) {
        try {
            String template = loadPromptTemplate();
            String prompt = String.format(template, applicant, remark, aiItemListStr);

            String aiAdvice = callDeepSeek(prompt);
            log.info("[AI 综合评估完成] 专家建议：\n{}", aiAdvice);

            if (aiAdvice.contains("高危")) {
                markAsAiHighRisk(recordIds, aiAdvice);
            }
        } catch (Exception e) {
            log.error("[AI 批量评估失败] {}", e.getMessage());
            throw e;
        }
    }

    // ==================== 私有工具方法（不变）====================

    private String loadPromptTemplate() {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/ai_risk_prompt.txt");
            return org.springframework.util.StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("读取 Prompt 模板失败", e);
            return "你是实验室安全专家。实验员 '%s' 申请用途：'%s'，清单：\n%s\n" +
                    "请评估风险并严格按以下格式输出：风险等级：[高危/中危/低危/安全]；危害描述：[xxx]；安全建议：[xxx]。";
        }
    }

    private String callDeepSeek(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepseekApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.containsKey("content")) {
                    return String.valueOf(message.get("content"));
                }
            }
        }
        return "";
    }

    private void markAsAiHighRisk(List<Long> recordIds, String aiAdvice) {
        log.warn("[AI 风控] 发现高危风险，正在标记 {} 条记录为待人工审批...", recordIds.size());
        for (Long recordId : recordIds) {
            ApproveDTO markDto = new ApproveDTO();
            markDto.setRecordId(recordId);
            markDto.setStatus(3);
            markDto.setReply("AI 风控识别高危风险: " + aiAdvice);

            recordService.markAiHighRisk(markDto);
        }
        log.info("[AI 风控] 标记完毕，等待管理员最终审批！");
    }
}