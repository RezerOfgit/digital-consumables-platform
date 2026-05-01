package com.dcp.service;

import com.dcp.entity.Category;
import com.dcp.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    public List<Category> getAllCategories() {
        return categoryMapper.selectList(null);
    }

    public void addCategory(Category category) {
        categoryMapper.insert(category);
    }
}