package com.dcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Re-zero
 * @version 1.0
 */
@Target(ElementType.METHOD) // 标明该注解只能贴在方法上
@Retention(RetentionPolicy.RUNTIME) // 标明在运行时生效
public @interface AuditLog {

    String module() default ""; // 操作模块，例如："耗材管理"
    String action() default ""; // 具体动作，例如："高危试剂领用"
}