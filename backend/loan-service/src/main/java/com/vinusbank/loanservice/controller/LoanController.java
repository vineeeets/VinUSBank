package com.vinusbank.loanservice.controller;

import com.vinusbank.loanservice.dto.LoanApplicationRequest;
import com.vinusbank.loanservice.dto.LoanResponse;
import com.vinusbank.loanservice.service.impl.LoanServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanServiceImpl loanService;

    @PostMapping("/calculate")
    public ResponseEntity<?> calculateEmi(
            @RequestParam BigDecimal principal,
            @RequestParam int tenureMonths) {
        log.info("[LOAN-CTRL] POST /calculate | Principal: ${} | Tenure: {} months", principal, tenureMonths);
        try {
            Map<String, Object> result = loanService.calculateEmi(principal, tenureMonths);
            log.debug("[LOAN-CTRL] EMI calculation returned successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[LOAN-CTRL] ✗ EMI calculation failed | Reason: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyForLoan(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody LoanApplicationRequest request) {
        log.info("[LOAN-CTRL] POST /apply | User: {} | Type: {} | Amount: ${} | Tenure: {} months",
                email, request.getLoanType(), request.getPrincipalAmount(), request.getTenureMonths());
        try {
            LoanResponse loan = loanService.applyForLoan(email, request);
            log.info("[LOAN-CTRL] ✓ Loan application processed | LoanNumber: {} | Status: {} | User: {}",
                    loan.getLoanNumber(), loan.getStatus(), email);
            return ResponseEntity.ok(Map.of("message", "Loan application submitted", "loan", loan));
        } catch (Exception e) {
            log.error("[LOAN-CTRL] ✗ Loan application failed | User: {} | Reason: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> getMyLoans(
            @RequestHeader("X-User-Email") String email) {
        log.info("[LOAN-CTRL] GET /loans | User: {}", email);
        return ResponseEntity.ok(loanService.getMyLoans(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[LOAN-CTRL] GET /loans/{} | User: {}", id, email);
        try {
            return ResponseEntity.ok(loanService.getLoanById(id, email));
        } catch (Exception e) {
            log.warn("[LOAN-CTRL] ✗ Loan not found | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
