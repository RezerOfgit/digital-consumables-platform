package com.dcp.aspect;

import com.dcp.annotation.AuditLog;
import com.dcp.entity.SysLog;
import com.dcp.mapper.SysLogMapper;
import com.dcp.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

/**
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Aspect      // 告诉 Spring 这是一个切面类
@Component   // 交给 Spring 容器管理
public class AuditLogAspect {

    @Resource
    private SysLogMapper sysLogMapper;

    // Jackson 自带，Spring Boot 自动配置好了，直接注入即可
    @Resource
    private ObjectMapper objectMapper;

    /**
     * @Around 环绕通知：拦截所有贴了 @AuditLog 注解的方法
     */
    @Around("@annotation(auditLog)") // auditLog 参数要和下面方法参数名一致
    public Object recordAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        // 1. 先让原本的业务逻辑执行 (比如扣库存、生成记录)
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 如果业务代码报错了，也要把错误日志记下来（可选，这里为了简单直接抛出）
            throw e;
        }

        // 2. 业务执行成功后，开始异步或者同步记录日志
        try {
            SysLog sysLog = new SysLog();
            // 【亮点】：通过 ThreadLocal 无侵入地获取当前操作人！业务方法的参数里根本不需要传 username！
            sysLog.setUsername(UserContext.getUser());
            sysLog.setModule(auditLog.module());
            sysLog.setAction(auditLog.action());

            // 获取方法的入参，转成 JSON 字符串存起来
            Object[] args = joinPoint.getArgs();
            sysLog.setParams(objectMapper.writeValueAsString(args));

            // 插入数据库
            sysLogMapper.insert(sysLog);
            log.info("📝 [安全审计] 已记录操作日志: {} - {}", sysLog.getUsername(), sysLog.getAction());

        } catch (Exception e) {
            log.error("❌ [安全审计] 记录日志失败", e);
        }

        return result; // 返回原本业务方法的执行结果
    }
}
