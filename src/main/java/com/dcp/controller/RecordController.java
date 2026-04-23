package com.dcp.controller;

import com.dcp.dto.ApplyDTO;
import com.dcp.dto.R;
import com.dcp.service.RecordService;
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

    @PostMapping("/apply")
    public R<Void> apply(@RequestBody ApplyDTO applyDTO) {
        // 最基础的参数校验
        if (applyDTO.getQuantity() == null || applyDTO.getQuantity() <= 0) {
            return R.fail("领用数量必须大于0");
        }

        recordService.applyMaterial(applyDTO);
        return R.ok("领用申请成功！", null);
    }
}