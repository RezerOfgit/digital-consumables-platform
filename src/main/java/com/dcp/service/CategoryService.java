package com.dcp.service;

import com.dcp.entity.Category;
import com.dcp.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 耗材分类服务
 * @author Re-zero
 * @version 1.0
 */
@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    /**
     * 查询所有分类
     * @return
     */
    public List<Category> getAllCategories() {
        return categoryMapper.selectList(null);
    }

    /**
     * 新增分类
     * @param category
     */
    public void addCategory(Category category) {
        categoryMapper.insert(category);
    }
}