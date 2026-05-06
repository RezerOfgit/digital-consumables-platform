package com.dcp.security;

import com.dcp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 用户详情实现：封装认证用户信息及角色。
 * @author Re-zero
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 将数据库 User 实体转换为 Security 认证对象。
     * @param user 数据库用户实体
     * @return Security 认证对象
     */
    public static UserDetailsImpl build(User user) {
        // 根据角色拼接 ROLE_ 前缀，Spring Security 鉴权规范要求
        String role = user.getRole().equals("ADMIN") ? "ROLE_ADMIN" : "ROLE_USER";
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
