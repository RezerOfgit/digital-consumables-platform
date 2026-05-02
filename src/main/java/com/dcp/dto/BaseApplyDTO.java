package com.dcp.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Re-zero
 * @version 1.0
 * 领用申请的公共基础 DTO
 */
@Data
public class BaseApplyDTO {

    @ApiModelProperty(value = "申请人姓名", required = true, example = "张三")
    private String applicant;

    @ApiModelProperty(value = "用途说明", example = "用于日常实验")
    private String remark;
}