package com.vinusbank.complianceservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String status; // PENDING, APPROVED, REJECTED
    private String reviewerNotes;
    
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
