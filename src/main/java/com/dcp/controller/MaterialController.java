package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.entity.Material;
import com.dcp.service.MaterialService;
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

    @PostMapping("/add")
    public R<Void> add(@RequestBody Material material) {
        materialService.addMaterial(material);
        return R.ok("耗材入库成功", null);
    }
}