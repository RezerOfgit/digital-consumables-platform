package com.dcp.aspect;

import com.dcp.annotation.RateLimit;
import com.dcp.exception.BusinessException;
import com.dcp.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

/**
 * 接口限流切面：基于 Redis + Lua 脚本实现用户级接口限流
 * @author Re-zero
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> redisScript;

    // 项目启动时将 Lua 脚本预加载到内存，避免每次调用重复读文件
    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setLocation(new ClassPathResource("lua/rate_limit.lua"));
    }

    /**
     * 环绕通知：拦截 @RateLimit 注解的方法，按用户粒度限流
     * @param joinPoint
     * @param rateLimit
     * @return
     * @throws Throwable
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 获取当前登录用户名，未登录则按匿名处理
        String username = UserContext.getUser();
        if (username == null) {
            username = "anonymous";
        }

        // 2. 构建限流 Key: rate_limit:类名:方法名:用户名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String key = String.format("rate_limit:%s:%s:%s", className, methodName, username);

        // 3. 执行 Lua 脚本，返回 0 表示被拦截，1 表示放行
        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(rateLimit.count()),
                String.valueOf(rateLimit.time())
        );

        // 4. 超出限流阈值时抛出异常
        if (result != null && result == 0L) {
            log.warn("[接口限流] 用户 {} 访问接口 {} 过于频繁", username, methodName);
            // 抛出业务异常，正好会被你刚才写好的 GlobalExceptionHandler 完美拦截！
            throw new BusinessException(429, "操作过于频繁，请稍后再试！");
        }

        return joinPoint.proceed();
    }
}
