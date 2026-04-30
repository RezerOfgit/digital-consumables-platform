package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.entity.Material;
import com.dcp.service.MaterialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Api(tags = "📦 耗材账目管理模块") // 【新增】给整个 Controller 命名
@RestController
@RequestMapping("/api/material")
public class MaterialController {
    @Resource
    private MaterialService materialService;

    @ApiOperation("查询耗材列表 (无权限限制)") // 【新增】给具体的接口命名
    @GetMapping("/list")
    public R<List<Material>> list() {
        return R.ok(materialService.listAll());
    }

    // 真正的耗材入库接口：只有 ADMIN 角色才能访问！
    @ApiOperation("耗材入库 (仅限 ADMIN)") // 【新增】给具体的接口命名
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> add(@RequestBody Material material) {
        // 执行真实的入库逻辑
        materialService.addMaterial(material);
        return R.ok("耗材入库成功", null);
    }
}