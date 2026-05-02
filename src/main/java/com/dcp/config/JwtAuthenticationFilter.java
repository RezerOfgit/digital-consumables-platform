package com.dcp.config;

import com.dcp.utils.JwtUtils;
import com.dcp.utils.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：解析请求头中的 Token，将用户信息写入 SecurityContext 和 ThreadLocal
 * @author Re-zero
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 从请求头中获取 Token
            String token = request.getHeader("Authorization");

            // 2. 校验 Token 格式
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                token = token.substring(7); // 剥离 "Bearer " 前缀，拿到真实的 JWT 字符串

                try {
                    // 3. 解析 Token，提取用户名和角色
                    Claims claims = jwtUtils.parseToken(token);
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);

                    // 4. 将认证信息写入 SecurityContext
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // ROLE_ 前缀是 Spring Security 角色鉴权的规范要求
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 将用户名写入 ThreadLocal，供下游 AOP 审计日志使用
                        UserContext.setUser(username);
                    }
                } catch (Exception e) {
                    // Token 无效时不做处理，由 Spring Security 后续拦截返回 403
                    logger.warn("JWT 校验失败: " + e.getMessage());
                }
            }

            // 5. 放行至下一个过滤器
            filterChain.doFilter(request, response);

        } finally {
            // 清理 ThreadLocal，防止 Tomcat 线程池复用导致的数据串线或内存泄漏
            UserContext.clear();
        }
    }
}