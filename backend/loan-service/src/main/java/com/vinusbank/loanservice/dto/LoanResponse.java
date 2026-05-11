package com.vinusbank.loanservice.dto;

import com.vinusbank.loanservice.entity.Loan;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanResponse {
    private String id;
    private String loanNumber;
    private String loanType;
    private String accountId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;
    private BigDecimal outstandingBalance;
    private String status;
    private String purpose;
    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;

    public static LoanResponse from(Loan loan) {
        LoanResponse dto = new LoanResponse();
        dto.setId(loan.getId());
        dto.setLoanNumber(loan.getLoanNumber());
        dto.setLoanType(loan.getLoanType().name());
        dto.setAccountId(loan.getAccountId());
        dto.setPrincipalAmount(loan.getPrincipalAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());
        dto.setEmiAmount(loan.getEmiAmount());
        dto.setTotalInterest(loan.getTotalInterest());
        dto.setTotalPayable(loan.getTotalPayable());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
        dto.setStatus(loan.getStatus().name());
        dto.setPurpose(loan.getPurpose());
        dto.setAppliedAt(loan.getAppliedAt());
        dto.setApprovedAt(loan.getApprovedAt());
        dto.setDisbursedAt(loan.getDisbursedAt());
        return dto;
    }
}
