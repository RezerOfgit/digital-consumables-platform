package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.entity.Category;
import com.dcp.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 耗材分类控制器。
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /** 查询所有分类 */
    @GetMapping("/list")
    public R<List<Category>> list() {
        List<Category> list = categoryService.getAllCategories();
        return R.ok(list);
    }

    /** 新增分类 */
    @PostMapping("/add")
    public R<Void> add(@RequestBody Category category) {
        categoryService.addCategory(category);
        return R.ok("分类添加成功", null);
    }
}