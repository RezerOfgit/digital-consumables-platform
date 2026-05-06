package com.dcp.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 领用申请的公共基础 DTO。
 * @author Re-zero
 * @version 1.0
 */
@Data
public class BaseApplyDTO {

    @ApiModelProperty(value = "申请人姓名", required = true, example = "张三")
    @NotBlank(message = "申请人姓名不能为空")
    private String applicant;

    @ApiModelProperty(value = "用途说明", example = "用于日常实验")
    @NotBlank(message = "用途说明不能为空")
    private String remark;
}