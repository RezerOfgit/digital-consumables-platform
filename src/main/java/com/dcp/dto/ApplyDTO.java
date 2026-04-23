package com.dcp.dto;

import lombok.Data;

/**
 * @author Re-zero
 * @version 1.0
 */
@Data
public class ApplyDTO {
    private Long materialId; // 要领哪种耗材
    private String applicant; // 谁领的
    private Integer quantity; // 领多少
    private String remark; // 备注
}