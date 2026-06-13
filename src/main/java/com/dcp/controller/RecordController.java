package com.dcp.controller;

import com.dcp.annotation.AuditLog;
import com.dcp.annotation.RateLimit;
import com.dcp.dto.ApplyDTO;
import com.dcp.dto.ApproveDTO;
import com.dcp.dto.BatchApplyDTO;
import com.dcp.dto.R;
import com.dcp.entity.MaterialRecord;
import com.dcp.mapper.RecordMapper;
import com.dcp.service.RecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 领用记录管理控制器：单品/批量领用及审批。
 * @author Re-zero
 * @version 1.0
 */
@Api(tags = "领用记录管理模块")
@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Resource
    private RecordService recordService;

    @Resource
    private RecordMapper recordMapper;

    /** 获取领用记录列表 */
    @ApiOperation("获取领用记录列表")
    @GetMapping("/list")
    @RateLimit(time = 10, count = 15) // 10秒内最多查15次，防恶意F5狂刷
    public R<List<MaterialRecord>> list() {
        List<MaterialRecord> list = recordService.getRecordList();
        return R.ok("获取记录列表成功", list);
    }

    /** 耗材单品领用申请 */
    @ApiOperation("耗材单品领用申请")
    @PostMapping("/apply")
    @AuditLog(module = "领用中心", action = "提交耗材领用申请")
    @RateLimit(time = 10, count = 2)// 限制该用户 10 秒内最多只能调用 2 次
    public R<Void> apply(@Valid @RequestBody ApplyDTO applyDTO) {

        recordService.applyMaterial(applyDTO);
        return R.ok("申请已提交，已进入风控与审批流程", null);
    }

    /** 耗材批量领用申请 */
    @ApiOperation("耗材批量领用申请")
    @PostMapping("/apply-batch")
    @AuditLog(module = "领用中心", action = "提交批量耗材领用申请")
    @RateLimit(time = 15, count = 2) // 限制该用户 15 秒内最多只能调用 2 次
    public R<Void> apply(@Valid @RequestBody BatchApplyDTO batchDTO) {

        recordService.applyBatchMaterial(batchDTO); // 调用批量方法
        return R.ok("申请已提交，已进入风控与审批流程", null);
    }

    /**
     * 审批领用申请，仅限管理员。
     * 人工审批通过或驳回后，需同步更新库存状态。
     */
    @ApiOperation("审批领用申请 (人工/仅限 ADMIN)")
    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "领用中心", action = "人工审批耗材领用")
    public R<Void> approve(@RequestBody ApproveDTO approveDTO) {
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