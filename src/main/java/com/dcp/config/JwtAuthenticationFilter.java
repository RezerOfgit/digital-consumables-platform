package com.dcp.config;

import com.dcp.utils.JwtUtils;
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
 * @author Re-zero
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    // 构造器注入 JwtUtils
    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从 HTTP 请求头中获取名叫 "Authorization" 的值
        String token = request.getHeader("Authorization");

        // 2. 检查是否带了 Token，并且是不是以 "Bearer " 开头的标准格式
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7); // 剥离 "Bearer " 前缀，拿到真实的 JWT 字符串

            try {
                // 3. 解析 Token (这里如果 Token 伪造或过期，会直接抛出异常被 catch 抓住)
                Claims claims = jwtUtils.parseToken(token);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // 4. 验证成功！把用户的信息作为“已认证的票据”存入 Spring Security 的大内总管（Context）里
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 加上 ROLE_ 前缀，这是 Spring Security 区分角色的底层规范
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 票据无效：啥也不干，放行给后面的原生保安处理，它查不到认证信息自然会报 403
                logger.warn("JWT 校验失败: " + e.getMessage());
            }
        }

        // 5. 本环节查验结束，放行给下一个过滤器
        filterChain.doFilter(request, response);
    }
}
