package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.entity.Material;
import com.dcp.service.MaterialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController {
    @Resource
    private MaterialService materialService;

    @GetMapping("/list")
    public R<List<Material>> list() {
        return R.ok(materialService.listAll());
    }

    // 真正的耗材入库接口：只有 ADMIN 角色才能访问！
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> add(@RequestBody Material material) {
        // 执行真实的入库逻辑
        materialService.addMaterial(material);
        return R.ok("耗材入库成功", null);
    }
}