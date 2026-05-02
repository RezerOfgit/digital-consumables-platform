package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
// 【关键】：让 Lombok 知道我们在比较和生成 ToString 时要包含父类的字段
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "单项耗材领用申请")
public class ApplyDTO extends BaseApplyDTO {

    @ApiModelProperty(value = "要领用的耗材ID", required = true, example = "3")
    private Long materialId;

    @ApiModelProperty(value = "领用数量", required = true, example = "10")
    private Integer quantity;
}