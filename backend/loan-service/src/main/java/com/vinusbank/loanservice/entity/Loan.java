package com.vinusbank.loanservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "loan_number", unique = true, nullable = false, length = 20)
    private String loanNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount", precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "total_interest", precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_payable", precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.APPLIED;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LoanType {
        PERSONAL, HOME, AUTO, EDUCATION, BUSINESS
    }

    public enum LoanStatus {
        APPLIED, UNDER_REVIEW, APPROVED, REJECTED, DISBURSED, ACTIVE, CLOSED
    }
}
