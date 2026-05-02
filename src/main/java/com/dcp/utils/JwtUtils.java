package com.dcp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：Token 签发与解析
 * @author Re-zero
 * @version 1.0
 */
@Component
public class JwtUtils {

    private static final String SECRET_KEY = "DCP_Secret_Key_2026"; // 签名密钥
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000 * 7; // 令牌有效期 7 天

    /**
     * 根据用户名和角色签发 Token
     * @param username
     * @param role
     * @return
     */
    public String createToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * 解析并校验 Token
     * @param token
     * @return
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
