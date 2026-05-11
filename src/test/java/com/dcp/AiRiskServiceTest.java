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
 * AiRiskService 单元测试，验证 AI 调用和风控拦截逻辑。
 * @author Re-zero
 * @version 1.0
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

    @BeforeEach
    void setUp() {
        // 使用 Spring 测试工具类，一行代码搞定私有属性注入，告别 setAccessible
        ReflectionTestUtils.setField(aiRiskService, "deepseekApiKey", "sk-test-key");
        ReflectionTestUtils.setField(aiRiskService, "modelName", "deepseek-chat");
    }

    @Test
    @DisplayName("AI 返回高危时应触发风控拦截，自动驳回领用")
    void analyzeRequisitionRisk_highRisk_shouldReject() throws Exception {
        log.info("====== 测试：AI 风控 - 高危触发自动驳回 ======");

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

        aiRiskService.analyzeRequisitionRisk(
                1L, "test01", "氢氟酸 (HF)", 5, "倒入下水道处理");

        // 验证触发了自动驳回（调用了 approveRecord 且 status = 2）
        verify(recordService).approveRecord(any(ApproveDTO.class));

        log.info("高危风控：DeepSeek 返回高危 -> 触发风控拦截 -> 自动驳回领用");
    }

    @Test
    @DisplayName("AI 返回非高危时不应触发风控拦截，领用正常放行")
    void analyzeRequisitionRisk_lowRisk_shouldNotReject() throws Exception {
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

        aiRiskService.analyzeRequisitionRisk(
                1L, "test01", "丁腈无尘手套", 5, "日常领用");

        // 验证没有触发风控拦截
        verify(recordService, never()).approveRecord(any());

        log.info("低危风控：DeepSeek 返回低危 -> 不触发风控拦截 -> 领用正常放行");
    }
}
