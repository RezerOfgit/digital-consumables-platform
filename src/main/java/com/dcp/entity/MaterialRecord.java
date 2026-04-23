package com.dcp.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
public class MaterialRecord {
    private Long id;
    private Long materialId;
    private String applicant;
    private Integer quantity;
    private Integer status; // 0-待审批, 1-已发料, 2-已驳回 (V1.0我们默认直接发料成功为1)
    private String remark;
    private Date createTime;
    private Date updateTime;
}
