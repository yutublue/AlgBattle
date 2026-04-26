package org.kob.backend.utils;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    public static final long JWT_TTL = 60 * 60 * 1000L * 24 * 14;  // 有效期14天
    public static final String JWT_KEY = "SDFGjhdsfalshdfHFdsjkdfsdfdsf121232131afasdfac";

    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");//生成一个唯一标识
    }

    public static String createJWT(String subject) {
        JwtBuilder builder = getJwtBuilder(subject, null, getUUID());//创建JWT, 参数中的subject就是用户信息
        return builder.compact();
    }

    private static JwtBuilder getJwtBuilder(String subject, Long ttlMillis, String uuid) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;//指定签名算法
        SecretKey secretKey = generalKey();//生成密钥
        //获取当前时间
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        //设置过期时间
        if (ttlMillis == null) {
            ttlMillis = JwtUtil.JWT_TTL;
        }

        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);

        //构建JWT
        return Jwts.builder()
                .setId(uuid)//唯一ID
                .setSubject(subject)//用户信息
                .setIssuer("sg")//构建者
                .setIssuedAt(now)//构建时间
                .signWith(signatureAlgorithm, secretKey)//构建算法和密钥
                .setExpiration(expDate);//过期时间
    }

    public static SecretKey generalKey() {
        byte[] encodeKey = Base64.getDecoder().decode(JwtUtil.JWT_KEY);//这里用的JWT_KEY是上面定义的那一长串字符串
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    //解析和验证JWT
    public static Claims parseJWT(String jwt) throws Exception {
        SecretKey secretKey = generalKey();
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
