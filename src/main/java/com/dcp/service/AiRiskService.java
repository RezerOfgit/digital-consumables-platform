package com.dcp.service;

import lombok.extern.slf4j.Slf4j;
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
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Service
public class AiRiskService {

    @Resource
    private RestTemplate restTemplate;

    //  这里需要填入你申请的 DeepSeek API Key
    private static final String DEEPSEEK_API_KEY = "sk-xxxxxxxxxxxxxxxxxxx";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    /**
     * @Async 注解代表这是一个异步方法！
     * 只要调用它，Spring 会立刻把它扔进线程池后台执行，主线程直接放行！
     */
    @Async
    public void analyzeRequisitionRisk(String applicant, String materialName, Integer quantity, String remark) {
        log.info("🚀 [异步风控线程启动] 开始对 {} 领用 {} 进行 AI 风险评估...", applicant, materialName);

        try {
            // 1. 构造发给 AI 的提示词 (Prompt)
            String prompt = String.format(
                    "你是一个实验室安全专家。实验员 '%s' 申请领用耗材 '%s'，数量：%d，用途说明：'%s'。" +
                            "请简短评估该操作的安全风险，并给出不超过50字的安全建议。",
                    applicant, materialName, quantity, remark
            );

            // 2. 构造 HTTP 请求头和参数 (兼容 OpenAI 格式)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(DEEPSEEK_API_KEY);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 3. 发送请求给 DeepSeek
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);

            // 4. 解析结果并模拟落盘 (实际项目中，你可以把这个建议更新到 record 表里)
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String aiAdvice = (String) message.get("content");

                log.info("✅ [AI 评估完成] 安全建议：{}", aiAdvice);
            }
        } catch (Exception e) {
            log.error("❌ [AI 评估失败] 网络异常或 Key 错误：{}", e.getMessage());
        }
    }
}