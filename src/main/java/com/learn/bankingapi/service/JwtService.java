package com.learn.bankingapi.service;

import com.learn.bankingapi.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

// getEmail? шо на рахунок номера телефону

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION;

    private Key getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user){
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public String extractUserId(String token){
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String userId){
        final String user = extractUserId(token);
        return (user.equals(userId) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token){
        return getClaims(token).getExpiration().before(new Date());
    }
}
