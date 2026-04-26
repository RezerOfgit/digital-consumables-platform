package com.dcp.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role; // ADMIN 或 USER
    private Date createTime;
}
