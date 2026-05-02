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
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Service
public class AiRiskService {

    @Resource
    private RestTemplate restTemplate;

    // 使用 @Value 注解从 application.yml 中动态读取 API Key
    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    // 动态读取模型版本名称
    @Value("${deepseek.model}")
    private String modelName;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    // 使用 @Lazy 延迟注入，防止 RecordService 和 AiRiskService 循环依赖启动报错
    @Resource
    @Lazy
    private RecordService recordService;

    @Resource
    private ResourceLoader resourceLoader;

    // ==================== 私有工具方法 ====================

    /**
     * 加载 AI 提示词模板
     *
     * @return
     */
    private String loadPromptTemplate() {
        try {
            // 【核心修复】：直接写出完整的类路径 org.springframework.core.io.Resource
            // 这样编译器就明确知道这里用的是文件资源，而不是注入注解了！
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:templates/ai_risk_prompt.txt");
            return org.springframework.util.StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("读取 Prompt 模板失败", e);
            // 兜底的默认提示词
            return "你是一个安全专家。请评估实验员 '%s' 申请用途：'%s'，清单：\n%s\n如果有严重危险请包含【高危拦截】。";
        }
    }

    /**
     * 【公共方法】发送请求给 DeepSeek 并返回 AI 回复文本
     *
     * @param prompt
     * @return
     */
    private String callDeepSeek(String prompt) {

        // 1. 构造 HTTP 请求头和参数
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 这里使用我们注入的 deepseekApiKey
        headers.setBearerAuth(deepseekApiKey);

        // 2. 构造请求参数，使用动态注入的模型名称
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. 发送请求给 DeepSeek
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
     * 【公共方法】执行熔断：驳回指定的领用记录
     */
    private void executeCircuitBreaker(List<Long> recordIds, String aiAdvice) {
        log.warn("[AI 熔断] 正在自动驳回 {} 条订单并退还库存...", recordIds.size());
        for (Long recordId : recordIds) {
            ApproveDTO rejectDto = new ApproveDTO();
            rejectDto.setRecordId(recordId);
            rejectDto.setStatus(2);
            rejectDto.setReply("AI 智能风控自动熔断: " + aiAdvice);
            // AI 模拟管理员执行驳回操作
            recordService.approveRecord(rejectDto);
        }
        log.info("[AI 熔断] 处理完毕，库存已安全退还！");
    }

    // ==================== 风控方法 ====================

    /**
     * 单种耗材风控
     *
     * @param recordId
     * @param applicant
     * @param materialName
     * @param quantity
     * @param remark
     */
    @Async
    public void analyzeRequisitionRisk(Long recordId, String applicant, String materialName, Integer quantity, String remark) {

        // 1. 先校验 Key 是否配置
        if (deepseekApiKey == null || deepseekApiKey.isBlank()) {
            log.warn("DeepSeek API Key 未配置，跳过 AI 风控评估");
            return;
        }

        log.info("[异步风控线程启动] 开始对 {} 领用 {} 进行 AI 风险评估...", applicant, materialName);

        // 后续调用 API 的逻辑
        try {
            // 1. 加载外部模板并构造发给 AI 的提示词 (Prompt)
            String template = loadPromptTemplate(); // 复用同一个模板

            // 2. 核心优化：把单品信息组装成批量模板需要的“清单”格式
            String singleItemList = String.format("- %s (数量: %d)", materialName, quantity);

            // 3. 填充模板：applicant, remark, 单品种组装的清单
            String prompt = String.format(template, applicant, remark, singleItemList);

            String aiAdvice = callDeepSeek(prompt);
            log.info("[AI 评估完成] 专家建议：\n{}", aiAdvice);

            // 【AI 智能熔断】
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
     * 批量耗材综合风控
     *
     * @param recordIds
     * @param applicant
     * @param remark
     * @param aiItemListStr
     */
    @Async
    public void analyzeBatchRisk(List<Long> recordIds, String applicant, String remark, String aiItemListStr) {
        log.info("[异步风控线程启动] 开始对 {} 个耗材进行 AI 批量综合风控...", recordIds.size());

        try {
            // 1. 加载外部模板
            String template = loadPromptTemplate();

            // 2. 将数据填入模板 (%s 按顺序替换)
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