package com.vinusbank.cardservice.entity;

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
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "card_number_last_four", nullable = false, length = 4)
    private String cardNumberLastFour;

    @Column(name = "card_number_masked", nullable = false)
    private String cardNumberMasked;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    @Builder.Default
    private CardType cardType = CardType.VIRTUAL_DEBIT;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "daily_spend_limit", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dailySpendLimit = new BigDecimal("5000.00");

    @Column(name = "online_enabled")
    @Builder.Default
    private Boolean onlineEnabled = true;

    @Column(name = "international_enabled")
    @Builder.Default
    private Boolean internationalEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.PENDING;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CardType {
        VIRTUAL_DEBIT
    }

    public enum CardStatus {
        PENDING, ACTIVE, BLOCKED, CANCELLED
    }
}
