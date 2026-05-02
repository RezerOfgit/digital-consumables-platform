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

    // 项目启动时，提前把 Lua 脚本加载到内存中，提升性能
    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setLocation(new ClassPathResource("lua/rate_limit.lua"));
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 获取当前登录用户名（如果未登录则按匿名用户处理）
        String username = UserContext.getUser();
        if (username == null) {
            username = "anonymous";
        }

        // 2. 构建粒度极细的 Redis Key: rate_limit:类名:方法名:用户名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String key = String.format("rate_limit:%s:%s:%s", className, methodName, username);

        // 3. 执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(rateLimit.count()),
                String.valueOf(rateLimit.time())
        );

        // 4. 结果判断：0 表示被拦截，1 表示放行
        if (result != null && result == 0L) {
            log.warn("🚨 [接口限流] 用户 {} 访问接口 {} 过于频繁", username, methodName);
            // 抛出业务异常，正好会被你刚才写好的 GlobalExceptionHandler 完美拦截！
            throw new BusinessException(429, "操作过于频繁，请稍后再试！");
        }

        // 放行，执行实际的业务方法
        return joinPoint.proceed();
    }
}
