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
@Data // 自动生成 getters, setters, toString
@ApiModel(description = "耗材分类实体")
@TableName("category")
public class Category {

    @ApiModelProperty(value = "分类主键ID", hidden = true)
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "分类名称（如：万级洁净室防护用品）", required = true, example = "锂电池研发高危试剂")
    private String name;

    @ApiModelProperty(value = "排序权重（数字越小越靠前）", example = "10")
    private Integer sort;

    @ApiModelProperty(value = "创建时间", hidden = true)
    private Date createTime;

    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateTime;
}