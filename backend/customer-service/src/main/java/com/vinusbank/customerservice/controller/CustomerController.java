package com.vinusbank.customerservice.controller;

import com.vinusbank.customerservice.entity.Customer;
import com.vinusbank.customerservice.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/profile")
    public ResponseEntity<?> createOrUpdateProfile(
            @RequestHeader("X-User-Email") String email,
            @RequestBody Customer request) {

        log.info("[CUSTOMER-CTRL] POST /profile | User: {}", email);

        if (email == null || email.isEmpty()) {
            log.warn("[CUSTOMER-CTRL] ✗ Profile update failed — Email header missing");
            return ResponseEntity.status(401).body("Error: Email header missing from Gateway");
        }

        if (request.getFirstName() == null || request.getLastName() == null) {
            log.warn("[CUSTOMER-CTRL] ✗ Profile update failed — Missing required fields | User: {}", email);
            return ResponseEntity.badRequest().body("Error: firstName and lastName are required fields!");
        }

        Optional<Customer> existingProfileOpt = customerRepository.findByEmail(email);

        Customer targetCustomer;
        if (existingProfileOpt.isPresent()) {
            log.debug("[CUSTOMER-CTRL] Updating existing profile | User: {}", email);
            targetCustomer = existingProfileOpt.get();
            targetCustomer.setFirstName(request.getFirstName());
            targetCustomer.setLastName(request.getLastName());
            targetCustomer.setPhoneNumber(request.getPhoneNumber());
            targetCustomer.setAddress(request.getAddress());
        } else {
            log.debug("[CUSTOMER-CTRL] Creating new profile | User: {}", email);
            targetCustomer = Customer.builder()
                    .email(email)
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .address(request.getAddress())
                    .kycStatus("PENDING") 
                    .build();
        }

        customerRepository.save(targetCustomer);
        log.info("[CUSTOMER-CTRL] ✓ Profile saved successfully | User: {}", email);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer profile saved successfully");
        response.put("profile", targetCustomer);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("X-User-Email") String email) {
        log.info("[CUSTOMER-CTRL] GET /profile | User: {}", email);

        Optional<Customer> profile = customerRepository.findByEmail(email);

        if (profile.isPresent()) {
            log.debug("[CUSTOMER-CTRL] Profile retrieved | User: {}", email);
            return ResponseEntity.ok(profile.get());
        }

        log.warn("[CUSTOMER-CTRL] ✗ Profile not found | User: {}", email);
        return ResponseEntity.status(404).body("Profile not found for this user. Please complete registration.");
    }
}

