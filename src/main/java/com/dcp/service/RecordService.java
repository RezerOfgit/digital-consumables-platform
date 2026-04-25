package com.dcp.service;

import com.dcp.dto.ApplyDTO;
import com.dcp.entity.Material;
import com.dcp.entity.MaterialRecord;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import com.dcp.mapper.RecordMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author Re-zero
 * @version 1.0
 */
@Service
public class RecordService {

    @Resource
    private RecordMapper recordMapper;

    // 注入耗材的 Mapper，因为我们需要查库存、扣库存
    @Resource
    private MaterialMapper materialMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 这个常量必须和预热时的一致
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /**
     * 提交领用申请 (核心业务)
     * @Transactional 注解保证了下面的操作要么全部成功，要么全部回滚！
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyMaterial(ApplyDTO applyDTO) {
        String redisKey = STOCK_KEY_PREFIX + applyDTO.getMaterialId();

        // 1. 【高并发核心】Redis 原子预扣减库存
        // decrement 方法利用了 Redis 的 DECRBY 命令，它是原子性的，绝不会超卖
        Long remainStock = redisTemplate.opsForValue().decrement(redisKey, applyDTO.getQuantity());

        // 2. 检查扣减后的库存
        if (remainStock == null) {
            // 如果返回 null，说明 Redis 里压根没有这个键（可能预热失败或没这个耗材）
            throw new BusinessException("耗材缓存未命中或不存在！");
        }

        if (remainStock < 0) {
            // 扣完了发现变成负数了，说明库存不足！
            // 【重要补偿机制】：既然没货，我们得把刚才多扣的库存加回去（原子递增）
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity());
            throw new BusinessException("手慢了，该耗材已被抢空或库存不足！");
        }

        // 3. Redis 扣减成功后，再让 MySQL 去慢慢扣减真实库存
        // 这一步依然有 @Transactional 保护，如果报错，整体回滚
        int updatedRows = materialMapper.updateStock(applyDTO.getMaterialId(), -applyDTO.getQuantity());
        if (updatedRows == 0) {
            // 理论上只要 Redis 没问题，这里不会报错，但为了严谨还是要校验
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("数据库落盘失败，请重试！");
        }

        // 4. 生成 MySQL 领用记录
        MaterialRecord record = new MaterialRecord();
        record.setMaterialId(applyDTO.getMaterialId());
        record.setApplicant(applyDTO.getApplicant());
        record.setQuantity(applyDTO.getQuantity());
        record.setRemark(applyDTO.getRemark());
        record.setStatus(1);

        recordMapper.insert(record);
    }
}