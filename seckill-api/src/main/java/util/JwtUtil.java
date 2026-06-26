package util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Utility
 */
@Slf4j
public class JwtUtil {

    // HS512 requires key size >= 512 bits (64 bytes), Base64-encoded
    private static final String SECRET_BASE64 = "Ocr4iVbAHZiI0rwSwx5TYwBeIK54TY47E1vgU+O/hUeMjbJRE01K14yzwkt+58JggPxIKA9sTUxfUTuXBEk5NQ==";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
    private static final long EXPIRATION = 86400000L; // 24 hours

    /**
     * Generate JWT Token
     */
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Parse JWT Token and get claims
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get userId from token
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        //由于claims是hash<String ,Object>结构，所以取出来是Object对象
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        //这里需要强转是因为从前端获取的token是json格式，反序列化后得到的有可能是Long型（大数值）也可能是Integer（小数值）
        //使用Number可以优雅地吸收两个子类,统一转换成Long
        return null;
    }

    /**
     * Get username from token
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        //用clain来装token的三部分信息，借此来抓取过期时间
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
        //比较过期时间和当前时间，来判断是否过期
    }

    /**
     * Validate token
     */
    public static boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    private JwtUtil() {
    }
}
