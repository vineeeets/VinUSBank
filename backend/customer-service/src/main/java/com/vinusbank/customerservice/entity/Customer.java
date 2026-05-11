package com.vinusbank.customerservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This is explicitly passed down from the Gateway via the X-User-Email header
    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty("firstName")
    @Column(nullable = false)
    private String firstName;

    @JsonProperty("lastName")
    @Column(nullable = false)
    private String lastName;

    @Column
    private String phoneNumber;

    @Column
    private String address;

    // Explicit KYC field to align with High Level Design requirements
    @Column(nullable = false)
    private String kycStatus; // e.g., PENDING, APPROVED, REJECTED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
