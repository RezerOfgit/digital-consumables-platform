package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


import java.util.Date;

/**
 * 耗材实体类
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "耗材实体类")
@TableName("material")
public class Material {

    @ApiModelProperty(value = "乐观锁版本号", hidden = true)
    @Version // 更新时自动比对并累加 version
    private Integer version;

    @ApiModelProperty(value = "主键ID", hidden = true)
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