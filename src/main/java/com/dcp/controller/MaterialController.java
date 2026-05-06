package com.dcp.controller;

import com.dcp.annotation.AuditLog;
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
 * 耗材账目管理控制器。
 * @author Re-zero
 * @version 1.0
 */
@Api(tags = "耗材账目管理模块")
@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    @ApiOperation("查询耗材列表 (无权限限制)")
    @GetMapping("/list")
    public R<List<Material>> list() {
        return R.ok(materialService.listAll());
    }

    /** 耗材入库，仅限管理员操作 */
    @ApiOperation("耗材入库 (仅限 ADMIN)")
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "耗材管理", action = "新增耗材入库")
    public R<Void> add(@RequestBody Material material) {

        materialService.addMaterial(material);
        return R.ok("耗材入库成功", null);
    }
}