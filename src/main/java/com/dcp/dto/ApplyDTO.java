package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 单项耗材领用申请 DTO
 * @author Re-zero
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true) // 继承父类字段需显式声明
@ApiModel(description = "单项耗材领用申请")
public class ApplyDTO extends BaseApplyDTO {

    @ApiModelProperty(value = "要领用的耗材ID", required = true, example = "3")
    @NotNull(message = "耗材ID不能为空")
    private Long materialId;

    @ApiModelProperty(value = "领用数量", required = true, example = "10")
    @NotNull(message = "领用数量不能为空")
    @Min(value = 1, message = "领用数量至少为1")
    private Integer quantity;
}