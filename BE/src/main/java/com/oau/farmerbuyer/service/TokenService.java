package com.oau.farmerbuyer.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {
    private final byte[] jwtKey;
    private final int ttlMinutes;

    public TokenService(@Value("${app.security.jwtSecret}") String secret,
                        @Value("${app.security.jwtTtlMinutes}") int ttlMinutes) {
        this.jwtKey = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
    }

    public String issueJwt(Long userId, String phone, String role) {
        var now = Instant.now();
        var exp = now.plusSeconds(ttlMinutes * 60L);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("phone", phone)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(jwtKey), SignatureAlgorithm.HS256)
                .compact();
    }
}
