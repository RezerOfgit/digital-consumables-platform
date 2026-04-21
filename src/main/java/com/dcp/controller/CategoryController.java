package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.entity.Category;
import com.dcp.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    // 获取分类列表
    @GetMapping("/list")
    public R<List<Category>> list() {
        List<Category> list = categoryService.getAllCategories();
        return R.ok(list);
    }

    // 添加分类
    @PostMapping("/add")
    public R<Void> add(@RequestBody Category category) {
        categoryService.addCategory(category);
        return R.ok("分类添加成功", null);
    }
}