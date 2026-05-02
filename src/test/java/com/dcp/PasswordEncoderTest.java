package com.dcp;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码工具：用于生成 BCrypt 加密后的密码
 * @author Re-zero
 * @version 1.0
 */
public class PasswordEncoderTest {
    
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}