package com.dcp.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 批量领用申请中的单条耗材项。
 * @author Re-zero
 * @version 1.0
 */
@Data
public class ApplyItemDTO {

    @ApiModelProperty(value = "耗材ID", required = true)
    @NotNull(message = "耗材ID不能为空")
    private Long materialId;

    @ApiModelProperty(value = "领用数量", required = true)
    @NotNull(message = "领用数量不能为空")
    @Min(value = 1, message = "领用数量至少为1")
    private Integer quantity;
}
