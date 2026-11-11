package com.example.personalfinance.security;

import com.example.personalfinance.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        // Decoders.BASE64.decode(secret): đổi chuỗi Base64 trong file cấu hình thành mảng byte.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // biến mảng byte thành khóa dùng thuật toán HMAC.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder() // Tạo một JWT bằng “builder” — tức là thêm từng phần cho token.
                .subject(user.getEmail()) // Đặt subject (viết tắt là claim sub) thành email user. Khi đọc token, ta sẽ lấy email này để biết ai gửi request.
                .issuedAt(now) // thời điểm token được tạo.
                .expiration(expiration) // thời điểm token hết hạn.
                .signWith(getSigningKey()) // Dùng secret key để ký token. Nếu ai sửa email hoặc thời hạn ở trong token, chữ ký không còn hợp lệ và server sẽ từ chối token đó.
                .compact(); // Đóng gói tất cả thành chuỗi JWT hoàn chỉnh, gồm ba phần cách nhau bằng dấu chấm:
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parser() // tạo công cụ đọc token.
                .verifyWith(getSigningKey()) // bảo công cụ dùng secret key để kiểm tra chữ ký.
                .build()
                .parseSignedClaims(token) // đọc token, đồng thời kiểm tra token có bị sửa hay hết hạn không.
                .getPayload(); // lấy các claims như sub, iat, exp.

        return claims.getSubject();
    }
}