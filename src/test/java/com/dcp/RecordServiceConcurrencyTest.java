package com.dcp;

import com.dcp.dto.ApplyItemDTO;
import com.dcp.dto.BatchApplyDTO;
import com.dcp.entity.Material;
import com.dcp.mapper.MaterialMapper;
import com.dcp.service.RecordService;
import com.dcp.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高并发压测：40 线程同时领用同一耗材，验证 Redis 预扣 + MySQL 原子扣减的防超卖方案。
 * <p>核心验证维度：
 * <ul>
 *   <li>零超卖 —— 库存绝对不能为负数</li>
 *   <li>数据守恒 —— 初始库存 = 剩余库存 + 已消耗库存</li>
 * </ul>
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@SpringBootTest
public class RecordServiceConcurrencyTest {

    @Resource
    private RecordService recordService;

    @Resource
    private MaterialMapper materialMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 并发线程数 */
    private static final int THREAD_COUNT = 40;
    /** 目标耗材 ID */
    private static final Long TARGET_MATERIAL_ID = 1L;
    /** 测试用初始库存 */
    private static final int INITIAL_STOCK = 50;
    /** 每人领用数量 */
    private static final int PER_REQUEST_QUANTITY = 2;

    @Test
    public void testConcurrentApplyBatch() throws InterruptedException {

        // ===== 1. 同步初始化 MySQL 和 Redis 库存 =====
        Material mat = materialMapper.selectById(TARGET_MATERIAL_ID);
        mat.setStock(INITIAL_STOCK);
        materialMapper.updateById(mat);

        String redisKey = "dcp:material:stock:" + TARGET_MATERIAL_ID;
        redisTemplate.delete(redisKey);
        redisTemplate.opsForValue().set(redisKey, INITIAL_STOCK);

        log.info("[压测准备] 耗材 {} 初始库存设置为: {}", mat.getName(), INITIAL_STOCK);

        // ===== 2. 并发控制：CountDownLatch 发令枪模式 =====
        // readyLatch: 等待所有线程就绪
        // startLatch: 控制统一起跑时机（主线程 countDown 时所有线程同时释放）
        // endLatch:   等待所有线程执行完毕
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // ===== 3. 构造批量领用请求 =====
        BatchApplyDTO batchDTO = new BatchApplyDTO();
        batchDTO.setApplicant("并发测试员");
        batchDTO.setRemark("并发压测领用");
        ApplyItemDTO item = new ApplyItemDTO();
        item.setMaterialId(TARGET_MATERIAL_ID);
        item.setQuantity(PER_REQUEST_QUANTITY);
        batchDTO.setItems(Collections.singletonList(item));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        log.info("[压测开始] {} 线程瞬间并发领用，每人申请 {} 个", THREAD_COUNT, PER_REQUEST_QUANTITY);

        // ===== 4. 提交并发任务 =====
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadNum = i;
            executor.execute(() -> {
                try {
                    UserContext.setUser("test_user_" + threadNum);
                    readyLatch.countDown();
                    startLatch.await(); // 阻塞，等待发令枪

                    recordService.applyBatchMaterial(batchDTO);
                    successCount.incrementAndGet();
                    log.info("[线程 {}] 领用成功！", threadNum);

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("[线程 {}] 遗憾离场：{}", threadNum, e.getMessage());
                } finally {
                    UserContext.clear();
                    endLatch.countDown();
                }
            });
        }

        readyLatch.await();  // 等待所有线程就绪
        startLatch.countDown(); // 发令枪响，所有线程同时执行
        endLatch.await();  // 等待所有线程执行完毕
        executor.shutdown();

        // ===== 5. 结果验证 =====
        Material result = materialMapper.selectById(TARGET_MATERIAL_ID);
        int consumedStock = successCount.get() * PER_REQUEST_QUANTITY;

        log.info("=============================================");
        log.info("[压测结束] 成功: {}, 失败: {}, 最终库存: {}",
                successCount.get(), failCount.get(), result.getStock());
        log.info("=============================================");

        // 断言 1：零超卖 —— 库存绝对不能为负
        Assertions.assertTrue(result.getStock() >= 0,
                "发生超卖！库存为: " + result.getStock());

        // 断言 2：数据守恒 —— 初始库存 == 剩余 + 已消耗
        Assertions.assertEquals(INITIAL_STOCK, result.getStock() + consumedStock,
                "数据一致性被破坏！初始=" + INITIAL_STOCK
                        + " 剩余=" + result.getStock()
                        + " 消耗=" + consumedStock);
    }
}