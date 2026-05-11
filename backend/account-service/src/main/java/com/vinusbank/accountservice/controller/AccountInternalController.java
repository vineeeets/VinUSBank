package com.vinusbank.accountservice.controller;

import com.vinusbank.accountservice.entity.Account;
import com.vinusbank.accountservice.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Internal controller — called only by other microservices (transaction-service, loan-service)
 * via Feign. NOT routed through the API Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/internal/accounts")
public class AccountInternalController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<?> debit(
            @PathVariable String accountId,
            @RequestParam BigDecimal amount) {
        log.info("[INTERNAL] ▶ Debit request | AccountID: {} | Amount: ${}", accountId, amount);
        try {
            Account account = accountService.debitAccount(accountId, amount);
            log.info("[INTERNAL] ✓ Debit completed | AccountID: {} | NewBalance: ${}", accountId, account.getAvailableBalance());
            return ResponseEntity.ok(Map.of(
                    "accountId", account.getId(),
                    "newBalance", account.getAvailableBalance()
            ));
        } catch (Exception e) {
            log.error("[INTERNAL] ✗ Debit failed | AccountID: {} | Amount: ${} | Reason: {}", accountId, amount, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<?> credit(
            @PathVariable String accountId,
            @RequestParam BigDecimal amount) {
        log.info("[INTERNAL] ▶ Credit request | AccountID: {} | Amount: ${}", accountId, amount);
        try {
            Account account = accountService.creditAccount(accountId, amount);
            log.info("[INTERNAL] ✓ Credit completed | AccountID: {} | NewBalance: ${}", accountId, account.getAvailableBalance());
            return ResponseEntity.ok(Map.of(
                    "accountId", account.getId(),
                    "newBalance", account.getAvailableBalance()
            ));
        } catch (Exception e) {
            log.error("[INTERNAL] ✗ Credit failed | AccountID: {} | Amount: ${} | Reason: {}", accountId, amount, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<?> getByNumber(@PathVariable String accountNumber) {
        log.debug("[INTERNAL] Lookup by account number: {}", accountNumber);
        try {
            Account account = accountService.getAccountByNumber(accountNumber);
            log.debug("[INTERNAL] Found account: {} for number: {}", account.getId(), accountNumber);
            return ResponseEntity.ok(Map.of(
                    "id", account.getId(),
                    "accountNumber", account.getAccountNumber(),
                    "availableBalance", account.getAvailableBalance(),
                    "customerEmail", account.getCustomerEmail(),
                    "status", account.getStatus().name()
            ));
        } catch (Exception e) {
            log.warn("[INTERNAL] ✗ Account not found by number: {} | Reason: {}", accountNumber, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getById(@PathVariable String accountId) {
        log.debug("[INTERNAL] Lookup by ID: {}", accountId);
        try {
            Account account = accountService.getAccountEntityById(accountId);
            log.debug("[INTERNAL] Found account: {} | Owner: {}", accountId, account.getCustomerEmail());
            return ResponseEntity.ok(Map.of(
                    "id", account.getId(),
                    "accountNumber", account.getAccountNumber(),
                    "availableBalance", account.getAvailableBalance(),
                    "customerEmail", account.getCustomerEmail(),
                    "status", account.getStatus().name()
            ));
        } catch (Exception e) {
            log.warn("[INTERNAL] ✗ Account not found by ID: {} | Reason: {}", accountId, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
