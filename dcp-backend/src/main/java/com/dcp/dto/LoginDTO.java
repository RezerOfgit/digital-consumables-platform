package com.dcp.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户登录请求参数。
 * @author Re-zero
 * @version 1.0
 */
@Data
@ApiModel(description = "用户登录请求参数")
public class LoginDTO {

    @ApiModelProperty(value = "用户名", required = true, example = "admin")
    private String username;

    @ApiModelProperty(value = "密码", required = true, example = "******")
    private String password;
}
