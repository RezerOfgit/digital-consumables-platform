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

/**
 * 审计日志切面：拦截 @AuditLog 注解的方法，记录操作人、模块、入参
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Resource
    private SysLogMapper sysLogMapper;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 环绕通知：拦截所有贴了 @AuditLog 注解的方法
     * @param joinPoint
     * @param auditLog
     * @return
     * @throws Throwable
     */
    @Around("@annotation(auditLog)") // auditLog 参数名需与方法参数一致
    public Object recordAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        // 1. 先执行业务逻辑
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            throw e;
        }

        // 2. 业务执行成功后记录审计日志
        try {

            SysLog sysLog = new SysLog();
            // 通过 ThreadLocal 获取当前操作人，无需在业务方法参数中传递 username
            sysLog.setUsername(UserContext.getUser());
            sysLog.setModule(auditLog.module());
            sysLog.setAction(auditLog.action());

            // 获取方法入参，序列化为 JSON 存储
            Object[] args = joinPoint.getArgs();
            sysLog.setParams(objectMapper.writeValueAsString(args));

            sysLogMapper.insert(sysLog);
            log.info("[安全审计] 已记录操作日志: {} - {}", sysLog.getUsername(), sysLog.getAction());
        } catch (Exception e) {
            log.error("[安全审计] 记录日志失败", e);
        }

        return result;
    }
}
