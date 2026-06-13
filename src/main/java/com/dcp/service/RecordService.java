package com.dcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dcp.dto.ApplyDTO;
import com.dcp.dto.ApplyItemDTO;
import com.dcp.dto.ApproveDTO;
import com.dcp.dto.BatchApplyDTO;
import com.dcp.entity.Material;
import com.dcp.entity.MaterialRecord;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import com.dcp.mapper.RecordMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 领用记录服务，实现 Redis 原子预扣 + MySQL 原子扣减的防超卖方案。
 * <p>单品领用采用乐观锁（@Version）方案，适合低并发场景；
 * <p>批量领用采用原子扣减（stock = stock - N WHERE stock >= N）方案，适合高并发场景。
 * @author Re-zero
 * @version 1.0
 */
@Service
public class RecordService {

    @Resource
    private RecordMapper recordMapper;

    @Resource
    private MaterialMapper materialMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AiRiskService aiRiskService;

    // 与 MaterialCacheService 中的前缀保持一致
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /**
     * 单品领用申请。
     * 流程：Redis 原子预扣 -> MySQL 乐观锁落盘 -> 生成待审批记录 -> 异步 AI 风控
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyMaterial(ApplyDTO applyDTO) {
        String redisKey = STOCK_KEY_PREFIX + applyDTO.getMaterialId();

        // 1. Redis 原子预扣减
        Long remainStock = redisTemplate.opsForValue().decrement(redisKey, applyDTO.getQuantity());

        if (remainStock == null) {
            // null 说明 Redis 中没有这个 Key (缓存预热失败或耗材不存在)
            throw new BusinessException("耗材缓存未命中或不存在！");
        }
        if (remainStock < 0) {
            // 库存不足，将已扣减的数量加回去
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity());
            throw new BusinessException("手慢了，该耗材已被抢空或库存不足！");
        }

        // 2. MySQL 乐观锁落盘
        Material material = materialMapper.selectById(applyDTO.getMaterialId());

        if (material == null) {
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("数据库耗材记录不存在！");
        }

        material.setStock(material.getStock() - applyDTO.getQuantity());

        // updateById 配合 @Version 自动生成:
        // UPDATE material SET stock=?, version=version+1 WHERE id=? AND version=查出来的值
        int updatedRows = materialMapper.updateById(material);

        if (updatedRows == 0) {
            // 乐观锁冲突: version 已被其他线程修改, 补偿 Redis 并提示用户重试
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("系统繁忙，数据库并发更新冲突，请重试！");
        }

        // 3. 生成领用记录 (状态: 待审批)
        MaterialRecord record = new MaterialRecord();
        record.setMaterialId(applyDTO.getMaterialId());
        record.setApplicant(applyDTO.getApplicant());
        record.setQuantity(applyDTO.getQuantity());
        record.setRemark(applyDTO.getRemark());
        record.setStatus(0);

        recordMapper.insert(record); // 插入后 MyBatis-Plus 自动回填主键 ID

        // 4. 触发异步 AI 风控 (不阻塞主流程)
        aiRiskService.analyzeRequisitionRisk(
                record.getId(),
                applyDTO.getApplicant(),
                material.getName(),
                applyDTO.getQuantity(),
                applyDTO.getRemark()
        );
    }

    /**
     * 批量提交领用申请 (核心业务，原子扣减方案)。
     * 遍历明细逐条扣减，任意一条失败则回滚全部已扣 Redis 库存，@Transactional 回滚 MySQL。
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyBatchMaterial(BatchApplyDTO batchDTO) {
        // 记录已成功扣减的 Redis 条目，用于异常时回滚
        Map<Long, Integer> redisRollbackMap = new HashMap<>();
        List<Long> generatedRecordIds = new ArrayList<>();
        StringBuilder aiItemList = new StringBuilder();

        try {
            for (ApplyItemDTO item : batchDTO.getItems()) {
                String redisKey = STOCK_KEY_PREFIX + item.getMaterialId();

                // 1. Redis 原子扣减
                Long remainStock = redisTemplate.opsForValue().decrement(redisKey, item.getQuantity());
                if (remainStock == null) {
                    throw new BusinessException("耗材 ID: " + item.getMaterialId() + " 缓存未命中！");
                }
                if (remainStock < 0) {
                    redisTemplate.opsForValue().increment(redisKey, item.getQuantity());
                    throw new BusinessException("耗材 ID: " + item.getMaterialId() + " 库存不足！");
                }
                redisRollbackMap.put(item.getMaterialId(), item.getQuantity());

                // 2. MySQL 原子扣减（数据库行锁串行执行，零冲突）
                int updatedRows = materialMapper.deductStock(item.getMaterialId(), item.getQuantity());
                if (updatedRows == 0) {
                    throw new BusinessException("耗材 ID: " + item.getMaterialId() + " 库存不足！");
                }

                // 3. 查询耗材名称（用于记录和风控报告）
                Material material = materialMapper.selectById(item.getMaterialId());

                // 4. 生成领用记录
                MaterialRecord record = new MaterialRecord();
                record.setMaterialId(item.getMaterialId());
                record.setApplicant(batchDTO.getApplicant());
                record.setQuantity(item.getQuantity());
                record.setRemark(batchDTO.getRemark());
                record.setStatus(0); // 待审批
                recordMapper.insert(record);

                generatedRecordIds.add(record.getId());
                aiItemList.append(String.format("- %s (数量: %d)\n", material.getName(), item.getQuantity()));
            }

        } catch (Exception e) {
            // 补偿已扣减的 Redis 库存，MySQL 由 @Transactional 自动回滚
            for (Map.Entry<Long, Integer> entry : redisRollbackMap.entrySet()) {
                String rollbackKey = STOCK_KEY_PREFIX + entry.getKey();
                redisTemplate.opsForValue().increment(rollbackKey, entry.getValue());
            }
            throw e;
        }

        // 5. 触发 AI 批量风控
        aiRiskService.analyzeBatchRisk(
                generatedRecordIds,
                batchDTO.getApplicant(),
                batchDTO.getRemark(),
                aiItemList.toString()
        );
    }

    /**
     * 审批领用记录 (同意或驳回)。
     * 驳回 (status=2) 时归还 Redis 和 MySQL 库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveRecord(ApproveDTO approveDTO) {
        MaterialRecord record = recordMapper.selectById(approveDTO.getRecordId());
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }

        // 只有状态为 0 (待审批) 的记录才能被处理
        if (record.getStatus() != 0) {
            throw new BusinessException("该记录已处理，无法重复审批");
        }

        record.setStatus(approveDTO.getStatus());
        record.setRemark(record.getRemark() + " | [审批结果]: " + approveDTO.getReply());
        recordMapper.updateById(record);

        // 驳回时归还库存
        if (approveDTO.getStatus() == 2) {
            // 归还 MySQL 库存 (同样走乐观锁)
            Material material = materialMapper.selectById(record.getMaterialId());
            material.setStock(material.getStock() + record.getQuantity());
            materialMapper.updateById(material);

            // 还原 Redis 库存
            String redisKey = STOCK_KEY_PREFIX + record.getMaterialId();
            redisTemplate.opsForValue().increment(redisKey, record.getQuantity());
        }
    }

    /**
     * 获取所有领用记录列表 (纯查询，加 readOnly 优化性能)
     */
    @Transactional(readOnly = true)
    public List<MaterialRecord> getRecordList() {
        LambdaQueryWrapper<MaterialRecord> wrapper = new LambdaQueryWrapper<>();

        // 按照创建时间倒序排，最新的记录在最上面
        wrapper.orderByDesc(MaterialRecord::getCreateTime);

        // 保护性限制：防止压测生成的历史数据过多导致查询卡顿或内存溢出
        wrapper.last("LIMIT 200");
        return recordMapper.selectList(wrapper);
    }

    /**
     * 获取待审批记录列表 (仅查询 status = 0)
     */
    @Transactional(readOnly = true)
    public List<MaterialRecord> getPendingRecords() {
        LambdaQueryWrapper<MaterialRecord> wrapper = new LambdaQueryWrapper<>();

        // 核心过滤：0 代表待审批
        wrapper.eq(MaterialRecord::getStatus, 0);
        // 排序：最新的申请排在最前面
        wrapper.orderByDesc(MaterialRecord::getCreateTime);

        return recordMapper.selectList(wrapper);
    }
}