package com.dcp.security;

import com.dcp.entity.User;
import com.dcp.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Re-zero
 * @version 1.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 去数据库查出真实的 User
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("找不到该用户: " + username);
        }
        // 2. 穿上马甲，转换成 Security 认识的 UserDetails
        return UserDetailsImpl.build(user);
    }
}
