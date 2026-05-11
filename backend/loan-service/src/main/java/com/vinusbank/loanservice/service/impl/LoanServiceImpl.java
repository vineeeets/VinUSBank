package com.vinusbank.loanservice.service.impl;

import com.vinusbank.loanservice.client.AccountServiceClient;
import com.vinusbank.loanservice.dto.LoanApplicationRequest;
import com.vinusbank.loanservice.dto.LoanResponse;
import com.vinusbank.loanservice.entity.Loan;
import com.vinusbank.loanservice.repository.LoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoanServiceImpl {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    private static final BigDecimal AUTO_APPROVE_THRESHOLD = new BigDecimal("10000.00");
    private static final BigDecimal BASE_INTEREST_RATE = new BigDecimal("0.0899");
    private static final AtomicInteger loanCounter = new AtomicInteger(1);

    @Transactional
    public LoanResponse applyForLoan(String customerEmail, LoanApplicationRequest request) {
        log.info("[LOAN] ▶ Application received | User: {} | Type: {} | Amount: ${} | Tenure: {} months | Account: {}",
                customerEmail, request.getLoanType(), request.getPrincipalAmount(),
                request.getTenureMonths(), request.getAccountId());

        Loan.LoanType type;
        try {
            type = Loan.LoanType.valueOf(request.getLoanType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[LOAN] ✗ Invalid loan type: '{}' | User: {}", request.getLoanType(), customerEmail);
            throw new RuntimeException("Invalid loan type: " + request.getLoanType());
        }

        // EMI Calculation
        BigDecimal monthlyRate = BASE_INTEREST_RATE.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        int n = request.getTenureMonths();
        BigDecimal principal = request.getPrincipalAmount();

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRpowN = onePlusR.pow(n, new MathContext(10));
        BigDecimal emi = principal.multiply(monthlyRate).multiply(onePlusRpowN)
                .divide(onePlusRpowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);

        log.info("[LOAN] EMI calculated | Monthly EMI: ${} | Total Payable: ${} | Total Interest: ${}",
                emi, totalPayable, totalInterest);

        boolean autoApprove = principal.compareTo(AUTO_APPROVE_THRESHOLD) < 0;
        Loan.LoanStatus initialStatus = autoApprove ? Loan.LoanStatus.DISBURSED : Loan.LoanStatus.UNDER_REVIEW;

        if (autoApprove) {
            log.info("[LOAN] ✓ AUTO-APPROVE triggered | Amount ${} is below $10,000 threshold", principal);
        } else {
            log.info("[LOAN] ⏳ UNDER_REVIEW | Amount ${} exceeds $10,000 threshold — manual review required", principal);
        }

        String loanNumber = generateLoanNumber();
        Loan loan = Loan.builder()
                .id(UUID.randomUUID().toString())
                .customerEmail(customerEmail)
                .accountId(request.getAccountId())
                .loanNumber(loanNumber)
                .loanType(type)
                .principalAmount(principal)
                .interestRate(BASE_INTEREST_RATE)
                .tenureMonths(n)
                .emiAmount(emi)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .outstandingBalance(totalPayable)
                .status(initialStatus)
                .purpose(request.getPurpose())
                .appliedAt(LocalDateTime.now())
                .approvedAt(autoApprove ? LocalDateTime.now() : null)
                .disbursedAt(autoApprove ? LocalDateTime.now() : null)
                .build();

        loanRepository.save(loan);
        log.info("[LOAN] ✓ Loan record saved | LoanNumber: {} | Status: {}", loanNumber, initialStatus);

        if (autoApprove) {
            try {
                log.info("[LOAN] ▶ Disbursing ${} to account: {} via Feign", principal, request.getAccountId());
                accountServiceClient.creditAccount(request.getAccountId(), principal);
                log.info("[LOAN] ✓ Disbursement successful | LoanNumber: {} → Account: {}", loanNumber, request.getAccountId());
            } catch (Exception e) {
                log.error("[LOAN] ✗ Disbursement FAILED | LoanNumber: {} | Account: {} | Reason: {}",
                        loanNumber, request.getAccountId(), e.getMessage());
                loan.setStatus(Loan.LoanStatus.APPROVED);
                loanRepository.save(loan);
                throw new RuntimeException("Loan approved but disbursement failed: " + e.getMessage());
            }
        }

        return LoanResponse.from(loan);
    }

    public List<LoanResponse> getMyLoans(String customerEmail) {
        log.debug("[LOAN] Fetching loans for user: {}", customerEmail);
        List<LoanResponse> loans = loanRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(LoanResponse::from)
                .collect(Collectors.toList());
        log.info("[LOAN] Found {} loan(s) for user: {}", loans.size(), customerEmail);
        return loans;
    }

    public LoanResponse getLoanById(String id, String customerEmail) {
        log.debug("[LOAN] Fetching loan | ID: {} | User: {}", id, customerEmail);
        Loan loan = loanRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[LOAN] ✗ Loan not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Loan not found");
                });
        return LoanResponse.from(loan);
    }

    public Map<String, Object> calculateEmi(BigDecimal principal, int tenureMonths) {
        log.info("[LOAN] EMI calculation request | Principal: ${} | Tenure: {} months", principal, tenureMonths);
        BigDecimal monthlyRate = BASE_INTEREST_RATE.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRpowN = onePlusR.pow(tenureMonths, new MathContext(10));
        BigDecimal emi = principal.multiply(monthlyRate).multiply(onePlusRpowN)
                .divide(onePlusRpowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayable.subtract(principal).setScale(2, RoundingMode.HALF_UP);

        log.info("[LOAN] EMI result | EMI: ${} | TotalInterest: ${} | TotalPayable: ${}", emi, totalInterest, totalPayable);
        return Map.of(
                "principal", principal,
                "tenureMonths", tenureMonths,
                "annualInterestRate", BASE_INTEREST_RATE,
                "monthlyEmi", emi,
                "totalInterest", totalInterest,
                "totalPayable", totalPayable
        );
    }

    private String generateLoanNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "LN" + date + String.format("%04d", loanCounter.getAndIncrement());
    }
}
