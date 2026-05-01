package com.dcp.controller;

import com.dcp.annotation.AuditLog;
import com.dcp.dto.ApplyDTO;
import com.dcp.dto.R;
import com.dcp.service.RecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Resource
    private RecordService recordService;

    @ApiOperation("耗材领用申请")
    @PostMapping("/apply")
    // 【点睛之笔】：只需要这一行代码，审计日志自动生成！
    @AuditLog(module = "领用中心", action = "提交耗材领用申请")
    public R<Void> apply(@RequestBody ApplyDTO applyDTO) {
        // 最基础的参数校验
        if (applyDTO.getQuantity() == null || applyDTO.getQuantity() <= 0) {
            return R.fail("领用数量必须大于0");
        }

        recordService.applyMaterial(applyDTO);
        return R.ok("领用申请成功！", null);
    }
}