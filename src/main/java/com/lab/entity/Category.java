package com.lab.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data // 魔法注解：自动生成 getters, setters, toString
public class Category {
    private Long id;
    private String name;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
}