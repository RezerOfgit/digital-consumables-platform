package com.dcp.controller;

import com.dcp.dto.LoginDTO;
import com.dcp.dto.R;
import com.dcp.mapper.UserMapper;
import com.dcp.security.UserDetailsImpl;
import com.dcp.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：登录接口及 Token 签发
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private AuthenticationManager authenticationManager;

    /**
     * 用户登录，验证通过后签发 JWT Token
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 1. 由 Security 框架调用 UserDetailsServiceImpl 完成密码比对
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        // 2. 认证通过，取出自定义的 UserDetails 实现
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 3. 生成 Token，剥离 ROLE_ 前缀后存入
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String token = jwtUtils.createToken(userDetails.getUsername(), role);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", userDetails.getUsername());
        result.put("role", role);

        return R.ok("登录成功", result);
    }
}