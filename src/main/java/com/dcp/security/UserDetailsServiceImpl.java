package com.dcp.security;

import com.dcp.entity.User;
import com.dcp.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户认证服务：根据用户名查询数据库并转换为 Security 认证对象
 * @author Re-zero
 * @version 1.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("找不到该用户: " + username);
        }
        // 转换为 Spring Security 可识别的 UserDetails 对象
        return UserDetailsImpl.build(user);
    }
}
