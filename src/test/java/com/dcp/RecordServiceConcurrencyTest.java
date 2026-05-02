package com.dcp;

import com.dcp.dto.ApplyDTO;
import com.dcp.service.RecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 并发测试：40 线程同时扣减同一耗材库存，验证 Redis 预扣 + MySQL 乐观锁是否防超卖
 * @author Re-zero
 * @version 1.0
 */
@SpringBootTest
public class RecordServiceConcurrencyTest {

    @Autowired
    private RecordService recordService;

    @Test
    public void testConcurrentStockDeduction() throws Exception {
        int threadCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                ApplyDTO dto = new ApplyDTO();
                dto.setMaterialId(3L);
                dto.setApplicant("test01");
                dto.setQuantity(1);
                dto.setRemark("并发测试");
                try {
                    recordService.applyMaterial(dto);
                    System.out.println("线程 " + Thread.currentThread().getName() + " 成功");
                } catch (Exception e) {
                    System.out.println("线程 " + Thread.currentThread().getName() + " 失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("====== 并发测试完成 ======");
    }
}