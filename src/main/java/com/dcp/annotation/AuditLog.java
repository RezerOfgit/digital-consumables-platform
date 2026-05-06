package com.dcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解，标注在需要记录操作日志的方法上。
 * @author Re-zero
 * @version 1.0
 */
@Target(ElementType.METHOD) // 限定作用范围为方法
@Retention(RetentionPolicy.RUNTIME) // 运行时通过反射读取
public @interface AuditLog {

    /** 操作模块，如 "耗材管理" */
    String module() default "";

    /** 具体动作，如 "高危试剂领用" */
    String action() default "";
}