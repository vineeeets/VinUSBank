package com.vinusbank.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT Utility Class
 * WHY WE NEED THIS: This class is the mathematical engine of our security system.
 * After a user successfully proves their password is correct, Spring Security hands over execution to this class.
 * HOW IT WORKS: It uses the super-secret hash code defined in `application.yml` to mathematically sign a string (the token).
 * This token contains the user's email and when the token expires. Because no one else has the secret key, no hacker can forge a token.
 */
@Component
public class JwtUtil {

    // Pulls the master hashing secret from application.yml
    @Value("${vinusbank.security.jwt.secret}")
    private String jwtSecret;

    // Defines how long the token is valid for (e.g., 1 hour before forcing re-login)
    @Value("${vinusbank.security.jwt.expiration-ms}")
    private int jwtExpirationMs;

    // Hashes our raw string secret into a Cryptographically secure Key object
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generates the actual Token String that Postman/Angular will save.
     */
    public String generateJwtToken(Authentication authentication) {
        // Extract the user details that Spring Security just validated
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        // Build the physical token
        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // The payload (email)
                .setIssuedAt(new Date()) // Current time
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Deadline
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // The cryptographic locker
                .compact(); // Convert to standard JWT string format
    }
}
