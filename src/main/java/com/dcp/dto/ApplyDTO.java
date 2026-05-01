package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材领用申请请求参数")
public class ApplyDTO {

    @ApiModelProperty(value = "要领用的耗材ID", required = true, example = "3")
    private Long materialId;

    @ApiModelProperty(value = "申请人姓名", required = true, example = "张三")
    private String applicant;

    @ApiModelProperty(value = "领用数量", required = true, example = "10")
    private Integer quantity;

    @ApiModelProperty(value = "用途说明", example = "用于徒手清洗桌面")
    private String remark;
}