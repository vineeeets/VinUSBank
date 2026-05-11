package com.vinusbank.transactionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reference_number", unique = true, nullable = false, length = 30)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "source_account_id", length = 36)
    private String sourceAccountId;

    @Column(name = "destination_account_id", length = 36)
    private String destinationAccountId;

    @Column(name = "source_account_number", length = 20)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", length = 20)
    private String destinationAccountNumber;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        INTERNAL_TRANSFER, ACH_TRANSFER, BILL_PAYMENT, DEPOSIT, WITHDRAWAL, LOAN_DISBURSEMENT, LOAN_REPAYMENT
    }

    public enum TransactionStatus {
        INITIATED, COMPLETED, FAILED, REVERSED
    }
}
