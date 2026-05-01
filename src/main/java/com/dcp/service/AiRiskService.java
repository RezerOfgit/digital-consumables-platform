package com.dcp.service;

import com.dcp.dto.ApproveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

    // 修改方法签名，把 recordId 传进来
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
            // 1. 构造发给 AI 的提示词 (Prompt)
            String prompt = String.format(
                    "【角色】你是实验室安全专家，熟悉 10万+ 化学品 MSDS 数据。\n" +
                            "【任务】实验员 '%s' 申请领用 \"%s\"，数量 %d %s，用途说明：\"%s\"。\n" +
                            "请执行以下步骤：\n" +
                            "1. 精准识别该化学品的物理化学特性及主要危害。\n" +
                            "2. 根据用途说明，判断该操作是否存在配伍禁忌或违规操作。\n" +
                            "3. 如果存在风险，用一句话（不超过40字）指出具体危害（必须提及该化学品的具体名称和特性）。\n" +
                            "4. 用一句话（不超过40字）给出可执行的安全建议。\n" +
                            "【输出格式】风险等级：[高危/中危/低危/安全]；危害描述：[xxx]；安全建议：[xxx]。",
                    applicant, materialName, quantity, "瓶", remark
            );

            // 2. 构造 HTTP 请求头和参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 这里使用我们注入的 deepseekApiKey
            headers.setBearerAuth(deepseekApiKey);

            // 3. 构造请求参数，使用动态注入的模型名称
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 4. 发送请求给 DeepSeek
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);

            // 5. 解析结果并输出日志
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String aiAdvice = (String) message.get("content");

                log.info("[AI 评估完成] 专家建议：\n{}", aiAdvice);

                // 【AI 智能熔断】
                if (aiAdvice.contains("高危")) {
                    log.warn("🚨 触发 AI 熔断机制！正在自动驳回订单并退还库存...");

                    ApproveDTO rejectDto = new ApproveDTO();
                    rejectDto.setRecordId(recordId);
                    rejectDto.setStatus(2); // 2-已驳回
                    rejectDto.setReply("AI 智能风控自动熔断: " + aiAdvice);

                    // AI 模拟管理员执行驳回操作
                    recordService.approveRecord(rejectDto);
                    log.info("🛡️ AI 熔断处理完毕，库存已安全退还！");
                }
            }
        } catch (Exception e) {
            log.error("[AI 评估失败] 网络异常或 Key 错误：{}", e.getMessage());
        }
    }
}