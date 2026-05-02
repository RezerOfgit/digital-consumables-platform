package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "批量耗材领用申请")
public class BatchApplyDTO extends BaseApplyDTO {

    @ApiModelProperty(value = "申请耗材明细列表", required = true)
    @Valid
    private List<ApplyItemDTO> items;
}