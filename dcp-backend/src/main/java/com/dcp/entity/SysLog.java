package com.dcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统操作日志实体，对应 sys_log 表。
 * @author Re-zero
 * @version 1.0
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人用户名 */
    private String username;

    /** 操作模块（如：耗材管理） */
    private String module;

    /** 操作类型（如：领用申请） */
    private String action;

    /** 方法入参（JSON格式） */
    private String params;

    /** 操作时间 */
    private Date createTime;

    @TableLogic
    private Integer isDeleted;
}