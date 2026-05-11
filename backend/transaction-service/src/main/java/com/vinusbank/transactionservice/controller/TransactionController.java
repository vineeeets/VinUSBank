package com.vinusbank.transactionservice.controller;

import com.vinusbank.transactionservice.dto.TransactionResponse;
import com.vinusbank.transactionservice.dto.TransferRequest;
import com.vinusbank.transactionservice.service.impl.TransactionServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionServiceImpl transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody TransferRequest request) {
        log.info("[TXN-CTRL] POST /transfer | User: {} | From: {} | To: {} | Amount: ${}",
                email, request.getSourceAccountId(), request.getDestinationAccountNumber(), request.getAmount());
        try {
            TransactionResponse response = transactionService.transfer(email, request);
            log.info("[TXN-CTRL] ✓ Transfer success | Ref: {} | User: {}", response.getReferenceNumber(), email);
            return ResponseEntity.ok(Map.of(
                    "message", "Transfer completed successfully",
                    "transaction", response
            ));
        } catch (Exception e) {
            log.error("[TXN-CTRL] ✗ Transfer failed | User: {} | Reason: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @RequestHeader("X-User-Email") String email) {
        log.info("[TXN-CTRL] GET /transactions | User: {}", email);
        return ResponseEntity.ok(transactionService.getHistory(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[TXN-CTRL] GET /transactions/{} | User: {}", id, email);
        try {
            return ResponseEntity.ok(transactionService.getById(id, email));
        } catch (Exception e) {
            log.warn("[TXN-CTRL] ✗ Not found | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
