package com.dcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Re-zero
 * @version 1.0
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    // 限流时间窗口（单位：秒），默认 60 秒
    int time() default 60;

    // 时间窗口内允许的最大访问次数，默认 5 次
    int count() default 5;
}
