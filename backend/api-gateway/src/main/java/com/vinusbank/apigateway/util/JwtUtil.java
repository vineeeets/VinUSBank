package com.vinusbank.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    // Must map exactly to the exact same secret used in the Auth Service
    @Value("${vinusbank.security.jwt.secret}")
    private String jwtSecret;

    // Load the key algorithm identically as Auth service
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Parses the incoming JWT Token and strictly throws an Exception if tampered with or expired.
     */
    public void validateToken(final String token) {
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
    }
    
    /**
     * Extracts the email from inside the token payload
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
