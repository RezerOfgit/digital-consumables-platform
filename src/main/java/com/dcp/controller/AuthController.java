package com.dcp.controller;

import com.dcp.dto.LoginDTO;
import com.dcp.dto.R;
import com.dcp.entity.User;
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

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 1. 让 Security 拿着账号密码去调用我们的 UserDetailsServiceImpl 进行比对
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        // 2. 验证成功后，获取我们穿了马甲的 UserDetails
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 3. 生成 Token (注意这里可以剥离 ROLE_ 前缀)
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String token = jwtUtils.createToken(userDetails.getUsername(), role);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", userDetails.getUsername());
        result.put("role", role);

        return R.ok("登录成功", result);
    }
}