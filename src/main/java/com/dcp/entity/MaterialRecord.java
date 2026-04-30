package com.dcp.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材领用记录实体")
public class MaterialRecord {

    @ApiModelProperty(value = "记录主键ID", hidden = true)
    private Long id;

    @ApiModelProperty(value = "领用的耗材ID", required = true, example = "1")
    private Long materialId;

    @ApiModelProperty(value = "申请人工号/姓名", required = true, example = "张三")
    private String applicant;

    @ApiModelProperty(value = "领用数量", required = true, example = "5")
    private Integer quantity;

    @ApiModelProperty(value = "领用备注", example = "测试用，请勿线上审批")
    private String remark;

    @ApiModelProperty(value = "状态: 0-已提交待审批, 1-已通过(发料), 2-已驳回, 3-已归还")
    private Integer status;

    @ApiModelProperty(value = "申请时间", hidden = true)
    private Date createTime;

    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateTime;
}