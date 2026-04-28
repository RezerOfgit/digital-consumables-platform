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

//    @PostMapping("/add")
//    public R<Void> add(@RequestBody Material material) {
//        materialService.addMaterial(material);
//        return R.ok("耗材入库成功", null);
//    }

    // 【新增测试接口】：只有 ADMIN 角色才能访问！
//    @PostMapping("/add")
//    @PreAuthorize("hasRole('ADMIN')")
//    public R<String> addMaterial() {
//        // 里面先不写具体业务，只测试权限拦截
//        return R.ok("耗材入库成功！您是尊贵的 ADMIN 库管员！", null);
//    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")   // 权限控制加在这里
    public R<Void> add(@RequestBody Material material) {
        materialService.addMaterial(material);
        return R.ok("耗材入库成功", null);
    }
}