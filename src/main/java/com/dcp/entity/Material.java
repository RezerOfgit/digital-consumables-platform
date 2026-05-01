package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材实体类")
@TableName("material") // 告诉 MP 这个实体类对应数据库的 material 表
public class Material {

    @ApiModelProperty(value = "主键ID", hidden = true) // hidden=true 就会在文档的入参里隐藏
    // 告诉 MP 这是主键，且策略为数据库自增
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属分类ID", required = true, example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "耗材名称", required = true, example = "无尘手套")
    private String name;

    @ApiModelProperty(value = "规格型号", example = "9寸-麻面-M码")
    private String specification;

    @ApiModelProperty(value = "计量单位", required = true, example = "箱")
    private String unit;

    @ApiModelProperty(value = "当前库存量", example = "100")
    private Integer stock;

    @ApiModelProperty(value = "危险等级: 0-普通, 1-低危, 2-高危, 3-致命", example = "1")
    private Integer dangerLevel = 0;

    @ApiModelProperty(value = "存储条件", example = "常温避光")
    private String storageCondition;

    @ApiModelProperty(value = "创建时间", hidden = true)
    private Date createTime;

    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateTime;
}