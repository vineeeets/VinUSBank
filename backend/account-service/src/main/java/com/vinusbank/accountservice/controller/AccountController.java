package com.vinusbank.accountservice.controller;

import com.vinusbank.accountservice.dto.AccountResponse;
import com.vinusbank.accountservice.dto.OpenAccountRequest;
import com.vinusbank.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping
    public ResponseEntity<?> openAccount(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody OpenAccountRequest request) {
        log.info("[ACCOUNT-CTRL] POST /api/accounts | User: {} | Type: {}", email, request.getAccountType());
        try {
            AccountResponse account = accountService.openAccount(email, request);
            log.info("[ACCOUNT-CTRL] Account opened | Number: {} | User: {}", account.getAccountNumber(), email);
            return ResponseEntity.ok(Map.of("message", "Account opened successfully", "account", account));
        } catch (Exception e) {
            log.error("[ACCOUNT-CTRL] Open account failed | User: {} | Reason: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @RequestHeader("X-User-Email") String email) {
        log.info("[ACCOUNT-CTRL] GET /api/accounts | User: {}", email);
        return ResponseEntity.ok(accountService.getMyAccounts(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccountById(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[ACCOUNT-CTRL] GET /api/accounts/{} | User: {}", id, email);
        try {
            return ResponseEntity.ok(accountService.getAccountById(id, email));
        } catch (Exception e) {
            log.warn("[ACCOUNT-CTRL] Account not found | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[ACCOUNT-CTRL] GET /api/accounts/{}/balance | User: {}", id, email);
        try {
            AccountResponse acc = accountService.getBalance(id, email);
            log.debug("[ACCOUNT-CTRL] Balance returned | Account: {} | Available: ${}", acc.getAccountNumber(), acc.getAvailableBalance());
            return ResponseEntity.ok(Map.of(
                    "accountNumber", acc.getAccountNumber(),
                    "availableBalance", acc.getAvailableBalance(),
                    "currentBalance", acc.getCurrentBalance(),
                    "currency", acc.getCurrency()
            ));
        } catch (Exception e) {
            log.warn("[ACCOUNT-CTRL] Balance check failed | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeAccount(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[ACCOUNT-CTRL] POST /api/accounts/{}/close | User: {}", id, email);
        try {
            accountService.closeAccount(id, email);
            log.info("[ACCOUNT-CTRL] Account closure successful | ID: {} | User: {}", id, email);
            return ResponseEntity.ok(Map.of("message", "Account closure requested successfully"));
        } catch (Exception e) {
            log.error("[ACCOUNT-CTRL] Close failed | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
