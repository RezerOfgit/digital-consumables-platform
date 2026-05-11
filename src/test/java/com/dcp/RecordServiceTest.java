package com.dcp;

import com.dcp.dto.ApplyDTO;
import com.dcp.dto.ApplyItemDTO;
import com.dcp.dto.BatchApplyDTO;
import com.dcp.entity.Material;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import com.dcp.mapper.RecordMapper;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RecordService 单元测试，mock 外部依赖，专注验证业务逻辑。
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class RecordServiceTest {

    @InjectMocks
    private RecordService recordService;

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private MaterialMapper materialMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private AiRiskService aiRiskService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Material material;
    private ApplyDTO applyDTO;

    @BeforeEach
    void setUp() {
        material = new Material();
        material.setId(1L);
        material.setName("丁腈无尘手套");
        material.setStock(100);
        material.setVersion(0);

        applyDTO = new ApplyDTO();
        applyDTO.setMaterialId(1L);
        applyDTO.setApplicant("test01");
        applyDTO.setQuantity(5);
        applyDTO.setRemark("日常领用");
    }

    // ==================== applyMaterial（乐观锁方案）====================

    @Test
    @DisplayName("单品领用-正常扣减：Redis 预扣成功 + MySQL 乐观锁更新成功")
    void applyMaterial_success() {
        log.info("====== 测试：单品领用 - 正常扣减 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(95L);
        when(materialMapper.selectById(1L)).thenReturn(material);
        when(materialMapper.updateById(any(Material.class))).thenReturn(1);
        when(recordMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> recordService.applyMaterial(applyDTO));

        verify(valueOperations).decrement("dcp:material:stock:1", 5);
        verify(materialMapper).updateById(any(Material.class));
        verify(recordMapper).insert(any());
        verify(aiRiskService).analyzeRequisitionRisk(
                any(), eq("test01"), eq("丁腈无尘手套"), eq(5), eq("日常领用"));

        log.info("单品领用正常扣减：Redis 预扣 -> MySQL 乐观锁落盘 -> 生成记录 -> 触发风控");
    }

    @Test
    @DisplayName("单品领用-库存不足：Redis 返回负数，应补偿并抛异常")
    void applyMaterial_insufficientStock() {
        log.info("====== 测试：单品领用 - 库存不足 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(-1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.applyMaterial(applyDTO));

        assertEquals("手慢了，该耗材已被抢空或库存不足！", ex.getMessage());
        verify(valueOperations).increment("dcp:material:stock:1", 5);
        verify(materialMapper, never()).updateById(any());
        verify(recordMapper, never()).insert(any());

        log.info("库存不足拦截：Redis 预扣返回负数 -> 补偿 Redis -> 抛异常");
    }

    @Test
    @DisplayName("单品领用-乐观锁冲突：MySQL 更新失败，应补偿 Redis 并抛异常")
    void applyMaterial_optimisticLockConflict() {
        log.info("====== 测试：单品领用 - 乐观锁冲突 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(95L);
        when(materialMapper.selectById(1L)).thenReturn(material);
        when(materialMapper.updateById(any(Material.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.applyMaterial(applyDTO));

        assertTrue(ex.getMessage().contains("并发更新冲突"));
        verify(valueOperations).increment("dcp:material:stock:1", 5);
        verify(recordMapper, never()).insert(any());

        log.info("乐观锁冲突：Redis 预扣成功 -> MySQL version 不匹配 -> 补偿 Redis");
    }

    // ==================== applyBatchMaterial（原子扣减方案）====================

    @Test
    @DisplayName("批量领用-正常扣减：Redis 预扣成功 + MySQL 原子扣减成功")
    void applyBatchMaterial_success() {
        log.info("====== 测试：批量领用 - 正常扣减 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(45L);
        when(materialMapper.deductStock(1L, 2)).thenReturn(1);
        when(materialMapper.selectById(1L)).thenReturn(material);
        when(recordMapper.insert(any())).thenReturn(1);

        BatchApplyDTO batchDTO = new BatchApplyDTO();
        batchDTO.setApplicant("test01");
        batchDTO.setRemark("批量领用");
        ApplyItemDTO item = new ApplyItemDTO();
        item.setMaterialId(1L);
        item.setQuantity(2);
        batchDTO.setItems(Collections.singletonList(item));

        assertDoesNotThrow(() -> recordService.applyBatchMaterial(batchDTO));

        // 验证 Redis 扣减
        verify(valueOperations).decrement("dcp:material:stock:1", 2);
        // 验证 MySQL 原子扣减（不是 updateById）
        verify(materialMapper).deductStock(1L, 2);
        verify(materialMapper, never()).updateById(any());
        // 验证生成记录
        verify(recordMapper).insert(any());
        // 验证触发批量风控
        verify(aiRiskService).analyzeBatchRisk(any(), eq("test01"), eq("批量领用"), anyString());

        log.info("批量领用正常扣减：Redis 预扣 -> MySQL 原子扣减 -> 生成记录 -> 触发批量风控");
    }

    @Test
    @DisplayName("批量领用-MySQL 库存不足：deductStock 返回 0，应补偿 Redis 并抛异常")
    void applyBatchMaterial_insufficientStock() {
        log.info("====== 测试：批量领用 - MySQL 库存不足 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(45L);
        // deductStock 返回 0 表示 stock < quantity
        when(materialMapper.deductStock(1L, 2)).thenReturn(0);

        BatchApplyDTO batchDTO = new BatchApplyDTO();
        batchDTO.setApplicant("test01");
        batchDTO.setRemark("批量领用");
        ApplyItemDTO item = new ApplyItemDTO();
        item.setMaterialId(1L);
        item.setQuantity(2);
        batchDTO.setItems(Collections.singletonList(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.applyBatchMaterial(batchDTO));

        assertTrue(ex.getMessage().contains("库存不足"));
        // 验证补偿了 Redis
        verify(valueOperations).increment("dcp:material:stock:1", 2);
        // 验证没有生成记录
        verify(recordMapper, never()).insert(any());

        log.info("MySQL 库存不足：Redis 预扣成功 -> deductStock 返回 0 -> 补偿 Redis");
    }

    @Test
    @DisplayName("批量领用-Redis 库存不足：应直接拦截，不操作 MySQL")
    void applyBatchMaterial_redisInsufficient() {
        log.info("====== 测试：批量领用 - Redis 库存不足 ======");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement(anyString(), anyLong())).thenReturn(-1L);

        BatchApplyDTO batchDTO = new BatchApplyDTO();
        batchDTO.setApplicant("test01");
        batchDTO.setRemark("批量领用");
        ApplyItemDTO item = new ApplyItemDTO();
        item.setMaterialId(1L);
        item.setQuantity(2);
        batchDTO.setItems(Collections.singletonList(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.applyBatchMaterial(batchDTO));

        assertTrue(ex.getMessage().contains("库存不足"));
        // 验证补偿了 Redis
        verify(valueOperations).increment("dcp:material:stock:1", 2);
        // 验证没有操作 MySQL
        verify(materialMapper, never()).deductStock(anyLong(), anyInt());
        verify(recordMapper, never()).insert(any());

        log.info("Redis 库存不足：直接拦截 -> 补偿 Redis -> 不碰 MySQL");
    }
}