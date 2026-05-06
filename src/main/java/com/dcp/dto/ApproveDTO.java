package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 审批操作请求参数。
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "审批操作请求参数")
public class ApproveDTO {

    @ApiModelProperty(value = "领用记录ID", required = true, example = "1")
    private Long recordId;

    /** 审批结果：1-同意 2-驳回 */
    @ApiModelProperty(value = "审批结果: 1-同意, 2-驳回", required = true, example = "1")
    private Integer status;

    @ApiModelProperty(value = "审批意见", example = "AI判定高危，强制熔断")
    private String reply;
}
