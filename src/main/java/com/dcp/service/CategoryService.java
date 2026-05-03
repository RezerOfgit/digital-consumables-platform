package com.dcp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dcp.entity.Category;
import com.dcp.mapper.CategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 耗材分类服务
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private static final String CATEGORY_LIST_CACHE_KEY = "dcp:category:list";

    /**
     * 获取所有耗材分类（Redis 缓存 + 降级查库）
     * @return
     */
    public List<Category> getAllCategories() {
        try {
            // 1. 尝试从 Redis 缓存中获取
            String cacheData = stringRedisTemplate.opsForValue().get(CATEGORY_LIST_CACHE_KEY);

            if (StringUtils.hasText(cacheData)) {
                log.info("命中分类列表缓存");
                return objectMapper.readValue(cacheData, new TypeReference<List<Category>>() {});
            }
        } catch (Exception e) {
            // Redis 不可用时降级，不影响主业务
            log.error("读取分类缓存异常，触发降级查库", e);
        }

        // 2. 缓存未命中，查询 MySQL
        log.info("分类缓存未命中，执行数据库查询");
        List<Category> categoryList = categoryMapper.selectList(new QueryWrapper<>());

        // 3. 查询结果回写 Redis，设置 24 小时过期
        try {
            if (categoryList != null && !categoryList.isEmpty()) {
                String jsonStr = objectMapper.writeValueAsString(categoryList);
                stringRedisTemplate.opsForValue().set(CATEGORY_LIST_CACHE_KEY, jsonStr, 24, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("回写分类缓存失败", e);
        }
        
        return categoryList;
    }

    /**
     * 新增分类，同时清除缓存，保证下次查询获取最新数据
     * @param category
     */
    public void addCategory(Category category) {
        categoryMapper.insert(category);
        // 新增后清除缓存，下次查询时自动从 MySQL 重新加载
        try {
            stringRedisTemplate.delete(CATEGORY_LIST_CACHE_KEY);
        } catch (Exception e) {
            log.error("清除分类缓存失败", e);
        }
    }
}