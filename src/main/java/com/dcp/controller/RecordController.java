package com.dcp.controller;

import com.dcp.annotation.AuditLog;
import com.dcp.dto.ApplyDTO;
import com.dcp.dto.ApproveDTO;
import com.dcp.dto.R;
import com.dcp.service.RecordService;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @ApiOperation("审批领用申请 (人工/仅限 ADMIN)")
    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "领用中心", action = "人工审批耗材领用")
    public R<Void> approve(@RequestBody ApproveDTO approveDTO) {
        // 基础参数校验
        if (approveDTO.getRecordId() == null) {
            return R.fail("审批记录ID不能为空");
        }
        if (approveDTO.getStatus() == null || (approveDTO.getStatus() != 1 && approveDTO.getStatus() != 2)) {
            return R.fail("审批结果必须为 1-同意 或 2-驳回");
        }

        recordService.approveRecord(approveDTO);
        return R.ok("审批完成", null);
    }
}