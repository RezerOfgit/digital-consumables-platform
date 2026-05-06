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
 * 耗材库存缓存服务：启动时将 MySQL 库存预热到 Redis。
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

    // Redis 中库存 Key 的前缀规范
    private static final String STOCK_KEY_PREFIX = "dcp:material:stock:";

    /** 启动时预热：将全量库存从 MySQL 加载到 Redis */
    @PostConstruct
    public void initStockToRedis() {
        log.info("--- 开始进行耗材库存缓存预热 ---");
        List<Material> list = materialMapper.selectList(null);
        for (Material material : list) {
            String redisKey = STOCK_KEY_PREFIX + material.getId();
            redisTemplate.opsForValue().set(redisKey, material.getStock());
            log.info("已加载库存到 Redis -> 耗材[{}], 库存量: {}", material.getName(), material.getStock());
        }
        log.info("--- 耗材库存缓存预热完成 ---");
    }
}
