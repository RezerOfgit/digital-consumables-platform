package com.dcp;

import com.dcp.dto.ApproveDTO;
import com.dcp.service.AiRiskService;
import com.dcp.service.RecordService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiRiskService 单元测试，验证 MQ 消息发送和 AI 风控逻辑。
 * @author Re-zero
 * @version 2.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class AiRiskServiceTest {

    @InjectMocks
    private AiRiskService aiRiskService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RecordService recordService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiRiskService, "deepseekApiKey", "sk-test-key");
        ReflectionTestUtils.setField(aiRiskService, "modelName", "deepseek-chat");
    }

    @Test
    @DisplayName("单品风控应发送 MQ 消息到正确的交换机和队列")
    void analyzeRequisitionRisk_shouldSendMQMessage() {
        log.info("====== 测试：单品风控 MQ 消息发送 ======");

        aiRiskService.analyzeRequisitionRisk(
                1L, "test01", "氢氟酸 (HF)", 5, "倒入下水道处理");

        // 验证 MQ 消息发送到了正确的交换机和路由键
        verify(rabbitTemplate).convertAndSend(
                eq("dcp.ai.risk.exchange"),
                eq("ai.risk.single"),
                any(Map.class)
        );

        log.info("验证通过：消息已发送到 MQ 单品风控队列");
    }

    @Test
    @DisplayName("API Key 未配置时应跳过 MQ 发送")
    void analyzeRequisitionRisk_noApiKey_shouldSkip() {
        log.info("====== 测试：API Key 未配置跳过风控 ======");

        // 清空 API Key
        ReflectionTestUtils.setField(aiRiskService, "deepseekApiKey", "");

        aiRiskService.analyzeRequisitionRisk(
                1L, "test01", "氢氟酸 (HF)", 5, "倒入下水道处理");

        // 验证没有发送 MQ 消息
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));

        log.info("验证通过：API Key 为空时跳过风控");
    }

    @Test
    @DisplayName("AI 返回高危时 executeSingleRiskCheck 应标记为高危待审批")
    void executeSingleRiskCheck_highRisk_shouldMarkAsHighRisk() throws Exception {
        log.info("====== 测试：AI 风控 - 高危触发标记 ======");

        // mock prompt 模板加载失败，走兜底模板
        Resource mockResource = mock(Resource.class);
        when(mockResource.getInputStream()).thenThrow(new RuntimeException("文件不存在"));
        when(resourceLoader.getResource(anyString())).thenReturn(mockResource);

        // mock DeepSeek 返回"高危"
        Map<String, Object> message = new HashMap<>();
        message.put("content", "风险等级：高危；危害描述：氢氟酸属于剧毒强腐蚀性酸；安全建议：严禁倒入下水道");
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("choices", List.of(choice));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        aiRiskService.executeSingleRiskCheck(
                1L, "test01", "氢氟酸 (HF)", 5, "倒入下水道处理");

        // 验证标记为高危待审批（status = 3）
        verify(recordService).markAiHighRisk(any(ApproveDTO.class));

        log.info("验证通过：AI 返回高危 -> 标记为高危待审批");
    }

    @Test
    @DisplayName("AI 返回非高危时 executeSingleRiskCheck 不应触发标记")
    void executeSingleRiskCheck_lowRisk_shouldNotMark() throws Exception {
        log.info("====== 测试：AI 风控 - 低危正常放行 ======");

        Resource mockResource = mock(Resource.class);
        when(mockResource.getInputStream()).thenThrow(new RuntimeException("文件不存在"));
        when(resourceLoader.getResource(anyString())).thenReturn(mockResource);

        Map<String, Object> message = new HashMap<>();
        message.put("content", "风险等级：低危；危害描述：无显著风险；安全建议：常规操作即可");
        Map<String, Object> choice = new HashMap<>();
        choice.put("message", message);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("choices", List.of(choice));

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        aiRiskService.executeSingleRiskCheck(
                1L, "test01", "丁腈无尘手套", 5, "日常领用");

        // 验证没有触发标记
        verify(recordService, never()).markAiHighRisk(any());

        log.info("验证通过：AI 返回低危 -> 不触发标记 -> 正常放行");
    }
}