package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 耗材领用记录实体，对应 record 表。
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材领用记录实体")
@TableName("record")
public class MaterialRecord {

    @ApiModelProperty(value = "记录主键ID", hidden = true)
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "耗材ID", required = true, example = "1")
    private Long materialId;

    @ApiModelProperty(value = "申请人工号/姓名", required = true, example = "张三")
    private String applicant;

    @ApiModelProperty(value = "领用数量", required = true, example = "5")
    private Integer quantity;

    @ApiModelProperty(value = "领用备注", example = "用于有机合成实验")
    private String remark;

    /** 状态：0-待审批 1-已通过 2-已驳回 3-已归还 */
    @ApiModelProperty(value = "状态: 0-已提交待审批, 1-已通过(发料), 2-已驳回, 3-已归还")
    private Integer status;

    @ApiModelProperty(value = "创建时间", hidden = true)
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间", hidden = true)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @ApiModelProperty(value = "逻辑删除标志", hidden = true)
    @TableLogic
    private Integer isDeleted;
}