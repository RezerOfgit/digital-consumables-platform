package com.dcp.controller;

import com.dcp.dto.LoginDTO;
import com.dcp.dto.R;
import com.dcp.entity.User;
import com.dcp.mapper.UserMapper;
import com.dcp.utils.JwtUtils;
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

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 1. 查库
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null) return R.fail("用户不存在");

        // 2. 校验密码 (注意：不能用 == 判断，必须用 encoder.matches)
        if (!encoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return R.fail("密码错误");
        }

        // 3. 生成 Token
        String token = jwtUtils.createToken(user.getUsername(), user.getRole());

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("role", user.getRole());

        return R.ok("登录成功", result);
    }
}
