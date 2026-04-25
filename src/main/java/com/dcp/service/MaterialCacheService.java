package com.dcp.service;

import com.dcp.entity.Material;
import com.dcp.mapper.MaterialMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Service
public class MaterialCacheService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MaterialMapper materialMapper;

    // 定义 Redis 中库存 Key 的前缀规范
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /**
     * 缓存预热：在 Spring Boot 项目启动时，自动执行此方法
     * 把 MySQL 里的库存同步到 Redis 里
     */
    @PostConstruct
    public void initStockToRedis() {
        log.info("--- 开始进行耗材库存缓存预热 ---");
        List<Material> list = materialMapper.findAll();
        for (Material material : list) {
            String redisKey = STOCK_KEY_PREFIX + material.getId();
            // 将库存数存入 Redis
            redisTemplate.opsForValue().set(redisKey, material.getStock());
            log.info("已加载库存到 Redis -> 耗材[{}], 库存量: {}", material.getName(), material.getStock());
        }
        log.info("--- 耗材库存缓存预热完成 ---");
    }
}
