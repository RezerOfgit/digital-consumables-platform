package com.dcp.service;

import com.dcp.dto.ApplyDTO;
import com.dcp.dto.ApproveDTO;
import com.dcp.entity.Material;
import com.dcp.entity.MaterialRecord;
import com.dcp.exception.BusinessException;
import com.dcp.mapper.MaterialMapper;
import com.dcp.mapper.RecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource
    private AiRiskService aiRiskService;

    // 这个常量必须和预热时的一致
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /**
     * 提交领用申请 (核心业务)
     * @Transactional 注解保证了下面的操作要么全部成功，要么全部回滚！
     * @param applyDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyMaterial(ApplyDTO applyDTO) {
        String redisKey = STOCK_KEY_PREFIX + applyDTO.getMaterialId();

        // 1. Redis 原子预扣减库存
        // decrement 方法利用了 Redis 的 DECRBY 命令，它是原子性的，绝不会超卖
        Long remainStock = redisTemplate.opsForValue().decrement(redisKey, applyDTO.getQuantity());

        // 检查扣减后的库存
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

        // 2. MySQL 乐观锁落盘保护
        // 使用 MyBatis-Plus 的 selectById 查出当前耗材实体（包含当前最新的 version）
        Material material = materialMapper.selectById(applyDTO.getMaterialId());

        if (material == null) {
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("数据库耗材记录不存在！");
        }

        // 设置扣减后的真实数据库库存
        material.setStock(material.getStock() - applyDTO.getQuantity());

        // 使用 updateById 触发 MyBatis-Plus 的乐观锁机制
        // 底层 SQL 会自动附带：UPDATE ... SET stock = ?, version = version + 1 WHERE id = ? AND version = 原version
        int updatedRows = materialMapper.updateById(material);

        if (updatedRows == 0) {
            // 【核心亮点】如果 updatedRows == 0，说明在查出数据到更新的这几毫秒内，别的线程修改了这行数据！
            // 乐观锁生效，防御成功！我们必须回滚 Redis 库存并抛出异常。
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("系统繁忙，数据库并发更新冲突，请重试！");
        }

        //  3. 生成 MySQL 领用记录 (对接接下来的审批流)
        MaterialRecord record = new MaterialRecord();
        record.setMaterialId(applyDTO.getMaterialId());
        record.setApplicant(applyDTO.getApplicant());
        record.setQuantity(applyDTO.getQuantity());
        record.setRemark(applyDTO.getRemark());
        record.setStatus(0);

        recordMapper.insert(record); // MyBatis-Plus 插入后，会自动把生成的 ID 塞回 record 对象里

        // 触发异步 AI 风控审查！
        // 主线程走到这里，只是给线程池发了个通知，不需要等 AI 回复，直接就去 return 成功了！
        // 4. 异步 AI 风控（复用已查询的 material 对象）
        // 【触发 AI 熔断检查】，传入刚才生成的 record.getId()
        aiRiskService.analyzeRequisitionRisk(
                record.getId(),
                applyDTO.getApplicant(),
                material.getName(),
                applyDTO.getQuantity(),
                applyDTO.getRemark()
        );


    }

    /**
     * 审批领用记录（同意或驳回）
     * @param approveDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveRecord(ApproveDTO approveDTO) {
        // 1. 查出这笔领用记录
        MaterialRecord record = recordMapper.selectById(approveDTO.getRecordId());
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }

        // 2. 只有状态为 0 (待审批) 的记录才能被处理
        if (record.getStatus() != 0) {
            throw new BusinessException("该记录已处理，无法重复审批");
        }

        // 3. 更新状态和审批意见
        record.setStatus(approveDTO.getStatus());
        // 将原有的备注和新的审批意见拼起来
        record.setRemark(record.getRemark() + " | [审批结果]: " + approveDTO.getReply());
        recordMapper.updateById(record);

        // 4. 【核心闭环】：如果是驳回 (status == 2)，必须归还之前预扣的库存！
        if (approveDTO.getStatus() == 2) {
            // 还原 MySQL 库存 (这里同样会触发 MyBatis-Plus 的乐观锁版本号累加，非常安全)
            Material material = materialMapper.selectById(record.getMaterialId());
            material.setStock(material.getStock() + record.getQuantity());
            materialMapper.updateById(material);

            // 还原 Redis 缓存库存
            String redisKey = STOCK_KEY_PREFIX + record.getMaterialId();
            redisTemplate.opsForValue().increment(redisKey, record.getQuantity());
        }
    }
}