package com.dcp.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
public class ApplyItemDTO {
    @ApiModelProperty(value = "耗材ID", required = true)
    private Long materialId;

    @ApiModelProperty(value = "领用数量", required = true)
    private Integer quantity;
}
