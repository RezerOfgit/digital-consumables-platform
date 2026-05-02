package com.dcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * @author Re-zero
 * @version 1.0
 */
@Target(ElementType.METHOD) // 限定作用范围为方法
@Retention(RetentionPolicy.RUNTIME) // 运行时通过反射读取
public @interface AuditLog {

    String module() default ""; // 操作模块，例如："耗材管理"

    String action() default ""; // 具体动作，例如："高危试剂领用"
}