package com.dcp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：Token 签发与解析。
 * @author Re-zero
 * @version 1.0
 */
@Component
public class JwtUtils {

    @Value("${dcp.jwt.secret}")
    private String secretKey; // 签名密钥

    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000; // 令牌有效期 2 小时

    /**
     * 根据用户名和角色签发 Token。
     * @param username 用户名
     * @param role     角色标识
     * @return 签发的 JWT Token
     */
    public String createToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token，无效时抛出异常。
     * @param token JWT Token
     * @return 解析后的 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}
