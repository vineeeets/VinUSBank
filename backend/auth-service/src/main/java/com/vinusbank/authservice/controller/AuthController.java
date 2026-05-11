package com.vinusbank.authservice.controller;

import com.vinusbank.authservice.entity.User;
import com.vinusbank.authservice.repository.UserRepository;
import com.vinusbank.authservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Authentication Controller
 * WHY WE NEED THIS: This defines the exact /api/auth API endpoints that Postman
 * and Angular hit.
 * HOW IT WORKS: It maps JSON requests to Java methods, interacting with
 * databases and our Security config.
 */
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
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateJwtToken(authentication);
            
            log.info("[AUTH-CTRL] ✓ Login successful | User: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", jwt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("[AUTH-CTRL] ✗ Login failed | User: {} | Reason: {}", email, e.getMessage());
            throw e;
        }
    }
}

