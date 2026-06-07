package com.vinusbank.complianceservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_transaction_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyTransactionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ctrNumber;
    private String userEmail;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String status; // FILED, PENDING

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
