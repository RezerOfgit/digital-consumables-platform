package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 耗材分类实体，对应 category 表。
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材分类实体")
@TableName("category")
public class Category {

    @ApiModelProperty(value = "分类主键ID", hidden = true)
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "分类名称", required = true, example = "锂电池研发高危试剂")
    private String name;

    @ApiModelProperty(value = "排序权重，数字越小越靠前", example = "10")
    private Integer sort;

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