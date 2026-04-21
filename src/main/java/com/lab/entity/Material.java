package com.lab.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
public class Material {
    private Long id;
    private Long categoryId;
    private String name;
    private String specification;
    private String unit;
    private Integer stock;
    private Integer dangerLevel;
    private String storageCondition;
    private Date createTime;
    private Date updateTime;
}