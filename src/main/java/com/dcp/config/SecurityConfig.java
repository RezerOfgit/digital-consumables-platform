package com.dcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Re-zero
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF 防护（因为我们用的是 JWT，不需要这玩意）
                .csrf().disable()

                // 2. 开启跨域支持（让前端 Vue 能调通我们的接口）
                .cors().and()

                // 3. 设置 Session 机制为“无状态”（不使用传统的 Session，全靠 JWT）
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()

                // 4. 配置接口权限拦截规则
                .authorizeRequests()
                // 放行登录接口（所有人都能访问）
                .antMatchers("/api/auth/login").permitAll()
                // 暂时放行 Swagger 接口文档相关的路径（为 Day 7 留后路）
                .antMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs").permitAll()
                // 其他所有接口，必须经过认证（带上 Token）才能访问
                .anyRequest().authenticated();

        return http.build();
    }
}
