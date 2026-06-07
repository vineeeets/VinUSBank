package com.vinusbank.authservice.controller;

import com.vinusbank.authservice.entity.User;
import com.vinusbank.authservice.repository.UserRepository;
import com.vinusbank.authservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("[AUTH-CTRL] POST /register | Attempt for email: {}", email);
        
        if (userRepository.existsByEmail(email)) {
            log.warn("[AUTH-CTRL] ✗ Registration failed — Email already exists: {}", email);
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = User.builder()
                .email(email)
                .password(encoder.encode(request.get("password")))
                .role("ROLE_CUSTOMER")
                .mfaEnabled(false)
                .build();

        userRepository.save(user);
        log.info("[AUTH-CTRL] ✓ Registration successful | User: {}", email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("[AUTH-CTRL] POST /login | Attempt for email: {}", email);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.get("password")));
            
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isMfaEnabled()) {
                log.info("[AUTH-CTRL] MFA required for user: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "MFA required");
                response.put("mfaRequired", true);
                // We return success but indicate MFA is required. 
                // Client must call /api/auth/mfa/verify next.
                return ResponseEntity.ok(response);
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateJwtToken(authentication);
            
            log.info("[AUTH-CTRL] ✓ Login successful | User: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", jwt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("[AUTH-CTRL] ✗ Login failed | User: {} | Reason: {}", email, e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<?> setupMfa(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            // Authenticate user to ensure they are the owner
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.get("password")));

            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "MFA is already enabled"));
            }

            String secret = Base32.random();
            user.setMfaSecret(secret);
            userRepository.save(user);

            log.info("[AUTH-CTRL] MFA setup initialized for user: {}", email);

            // Return the secret so client can generate QR code
            // Format for authenticator apps: otpauth://totp/VinUSBank:email?secret=...&issuer=VinUSBank
            String qrUrl = String.format("otpauth://totp/VinUSBank:%s?secret=%s&issuer=VinUSBank", email, secret);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "MFA setup initialized");
            response.put("secret", secret);
            response.put("qrUrl", qrUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<?> enableMfa(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.get("password")));

            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "MFA is already enabled"));
            }

            Totp totp = new Totp(user.getMfaSecret());
            if (totp.verify(code)) {
                user.setMfaEnabled(true);
                userRepository.save(user);
                log.info("[AUTH-CTRL] MFA successfully enabled for user: {}", email);
                return ResponseEntity.ok(Map.of("message", "MFA enabled successfully"));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid MFA code"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.get("password")));

            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.isMfaEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "MFA is not enabled for this user"));
            }

            Totp totp = new Totp(user.getMfaSecret());
            if (totp.verify(code)) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtil.generateJwtToken(authentication);
                
                log.info("[AUTH-CTRL] ✓ MFA Login successful | User: {}", email);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("token", jwt);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid MFA code"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials or code"));
        }
    }
}
