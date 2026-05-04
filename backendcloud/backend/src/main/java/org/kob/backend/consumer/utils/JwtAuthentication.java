package org.kob.backend.consumer.utils;

import io.jsonwebtoken.Claims;
import org.kob.backend.utils.JwtUtil;

public class JwtAuthentication {
    public static Integer getUserId(String token){
        int userId = -1;
        try {
            Claims claims = JwtUtil.parseJWT(token);//使用Jwt验证token
            userId = Integer.parseInt(claims.getSubject());//subject是用户信息, 获取用户信息
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return userId;
    }

}
