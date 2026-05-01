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

    @Resource
    private AiRiskService aiRiskService;

    // 这个常量必须和预热时的一致
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /**
     * 提交领用申请 (核心业务)
     * @Transactional 注解保证了下面的操作要么全部成功，要么全部回滚！
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
//        Material material = materialMapper.selectById(applyDTO.getMaterialId());
        Material material = fetchMaterial(applyDTO.getMaterialId());

        if (material == null) {
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("数据库耗材记录不存在！");
        }

        // 设置扣减后的真实数据库库存
        material.setStock(material.getStock() - applyDTO.getQuantity());

        // 临时让当前线程睡 50ms，给其他请求创造并发机会
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 使用 updateById 触发 MyBatis-Plus 的乐观锁机制
        // 底层 SQL 会自动附带：UPDATE ... SET stock = ?, version = version + 1 WHERE id = ? AND version = 原version
        int updatedRows = materialMapper.updateById(material);

        if (updatedRows == 0) {
            // 【核心亮点】如果 updatedRows == 0，说明在查出数据到更新的这几毫秒内，别的线程修改了这行数据！
            // 乐观锁生效，防御成功！我们必须回滚 Redis 库存并抛出异常。
            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
            throw new BusinessException("系统繁忙，数据库并发更新冲突，请重试！");
        }

//        // 3. Redis 扣减成功后，再让 MySQL 去慢慢扣减真实库存
//        // 这一步依然有 @Transactional 保护，如果报错，整体回滚
//        int updatedRows = materialMapper.updateStock(applyDTO.getMaterialId(), -applyDTO.getQuantity());
//        if (updatedRows == 0) {
//            // 理论上只要 Redis 没问题，这里不会报错，但为了严谨还是要校验
//            redisTemplate.opsForValue().increment(redisKey, applyDTO.getQuantity()); // 补偿 Redis
//            throw new BusinessException("数据库落盘失败，请重试！");
//        }

        //  3. 生成 MySQL 领用记录 (对接接下来的审批流)
        MaterialRecord record = new MaterialRecord();
        record.setMaterialId(applyDTO.getMaterialId());
        record.setApplicant(applyDTO.getApplicant());
        record.setQuantity(applyDTO.getQuantity());
        record.setRemark(applyDTO.getRemark());

        // 【修改状态】从原本的 1(已发料) 修改为 0(待审批)
        record.setStatus(1);

        recordMapper.insert(record);

        // 触发异步 AI 风控审查！
        // 主线程走到这里，只是给线程池发了个通知，不需要等 AI 回复，直接就去 return 成功了！

        // 4. 异步 AI 风控（复用已查询的 material 对象）
//        aiRiskService.analyzeRequisitionRisk(
//                applyDTO.getApplicant(),
//                material.getName(),
//                applyDTO.getQuantity(),
//                applyDTO.getRemark()
//        );
    }

    /**
     * 单独查询耗材信息，不加事务，立刻返回。
     * 用来绕开数据库行锁，让并发请求都能拿到同一个初始 version。
     */
    public Material fetchMaterial(Long materialId) {
        return materialMapper.selectById(materialId);
    }
}