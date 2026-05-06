package com.dcp.service;

import com.dcp.dto.ApproveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风控服务，调用 DeepSeek 接口评估领用风险，高危时自动熔断驳回并退还库存。
 * 提示词模板外置在 classpath:templates/ai_risk_prompt.txt，支持无代码调整规则。
 * @author Re-zero
 * @version 1.0
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

    // @Lazy 延迟注入，防止与 RecordService 循环依赖导致启动报错
    @Resource
    @Lazy
    private RecordService recordService;

    @Resource
    private ResourceLoader resourceLoader;

    // ==================== 私有工具方法 ====================

    /**
     * 从 classpath 加载 AI 提示词模板，加载失败时返回兜底模板。
     */
    private String loadPromptTemplate() {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/ai_risk_prompt.txt");
            return org.springframework.util.StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("读取 Prompt 模板失败", e);
            // 兜底提示词，确保 AI 风控不会因模板丢失而完全失效
            return "你是实验室安全专家。实验员 '%s' 申请用途：'%s'，清单：\n%s\n" +
                    "请评估风险并严格按以下格式输出：风险等级：[高危/中危/低危/安全]；危害描述：[xxx]；安全建议：[xxx]。";
        }
    }

    /**
     * 调用 DeepSeek 接口，返回 AI 回复文本。
     */
    private String callDeepSeek(String prompt) {

        // 1. 构造 HTTP 请求头和参数
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepseekApiKey);

        // 2. 构造请求参数，使用动态注入的模型名称
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. 调用 DeepSeek API
        ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);

        // 4. 解析结果并输出日志
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.containsKey("content")) {
                    // 用 String.valueOf 避免 ClassCastException
                    return String.valueOf(message.get("content"));
                }
            }
        }
        return "";
    }

    /**
     * 熔断：批量驳回指定领用记录并退还库存。
     */
    private void executeCircuitBreaker(List<Long> recordIds, String aiAdvice) {
        log.warn("[AI 熔断] 正在自动驳回 {} 条订单并退还库存...", recordIds.size());
        for (Long recordId : recordIds) {
            ApproveDTO rejectDto = new ApproveDTO();
            rejectDto.setRecordId(recordId);
            rejectDto.setStatus(2);
            rejectDto.setReply("AI 智能风控自动熔断: " + aiAdvice);

            recordService.approveRecord(rejectDto);
        }
        log.info("[AI 熔断] 处理完毕，库存已安全退还！");
    }

    // ==================== 风控方法 ====================

    /**
     * 单种耗材异步风控，调用 DeepSeek 评估后决定是否熔断。
     */
    @Async
    public void analyzeRequisitionRisk(Long recordId, String applicant, String materialName, Integer quantity, String remark) {

        // API Key 未配置时跳过，不影响主流程
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过 AI 风控评估");
            return;
        }

        log.info("[异步风控线程启动] 开始对 {} 领用 {} 进行 AI 风险评估...", applicant, materialName);

        try {
            String template = loadPromptTemplate();
            String singleItemList = String.format("- %s (数量: %d)", materialName, quantity);
            String prompt = String.format(template, applicant, remark, singleItemList);

            // 调用 AI 并根据结果决定是否熔断
            String aiAdvice = callDeepSeek(prompt);
            log.info("[AI 评估完成] 专家建议：\n{}", aiAdvice);

            if (aiAdvice.contains("高危")) {
                executeCircuitBreaker(List.of(recordId), aiAdvice);
            }
        } catch (java.util.IllegalFormatException e) {
            log.error("[AI 模板错误] 提示词模板占位符与参数类型不匹配，请检查 ai_risk_prompt.txt 文件！详细原因：{}", e.getMessage());
        } catch (Exception e) {
            log.error("[AI 评估失败] 网络异常或 Key 错误：{}", e.getMessage());
        }
    }

    /**
     * 批量耗材异步综合风控，多项耗材合并为一次 AI 调用。
     */
    @Async
    public void analyzeBatchRisk(List<Long> recordIds, String applicant, String remark, String aiItemListStr) {
        log.info("[异步风控线程启动] 开始对 {} 个耗材进行 AI 批量综合风控...", recordIds.size());

        try {
            String template = loadPromptTemplate();
            String prompt = String.format(template, applicant, remark, aiItemListStr);

            String aiAdvice = callDeepSeek(prompt);
            log.info("[AI 综合评估完成] 专家建议：\n{}", aiAdvice);

            if (aiAdvice.contains("高危")) {
                executeCircuitBreaker(recordIds, aiAdvice);
            }
        } catch (Exception e) {
            log.error("[AI 批量评估失败] {}", e.getMessage());
        }
    }
}