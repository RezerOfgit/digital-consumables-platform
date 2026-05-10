package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 系统用户实体，对应 user 表。
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "系统用户实体")
@TableName("user")
public class User {

    @ApiModelProperty(value = "用户主键ID", hidden = true)
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "登录账号", required = true, example = "admin")
    private String username;

    @ApiModelProperty(value = "登录密码", required = true)
    private String password;

    @ApiModelProperty(value = "真实姓名", example = "系统管理员")
    private String realName;

    @ApiModelProperty(value = "角色: USER-实验员, ADMIN-库管员")
    private String role;

    @ApiModelProperty(value = "创建时间", hidden = true)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date createTime;

    @ApiModelProperty(value = "逻辑删除标志", hidden = true)
    @TableLogic
    private Integer isDeleted;
}