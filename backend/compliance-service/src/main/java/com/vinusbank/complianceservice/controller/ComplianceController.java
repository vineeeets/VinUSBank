package com.vinusbank.complianceservice.controller;

import com.vinusbank.complianceservice.entity.AuditTrail;
import com.vinusbank.complianceservice.entity.CurrencyTransactionReport;
import com.vinusbank.complianceservice.entity.KycReview;
import com.vinusbank.complianceservice.entity.SuspiciousActivityReport;
import com.vinusbank.complianceservice.repository.AuditTrailRepository;
import com.vinusbank.complianceservice.repository.CurrencyTransactionReportRepository;
import com.vinusbank.complianceservice.repository.KycReviewRepository;
import com.vinusbank.complianceservice.repository.SuspiciousActivityReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    @Autowired
    private KycReviewRepository kycRepository;
    
    @Autowired
    private SuspiciousActivityReportRepository sarRepository;
    
    @Autowired
    private CurrencyTransactionReportRepository ctrRepository;
    
    @Autowired
    private AuditTrailRepository auditRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // --- KYC Endpoints ---

    @GetMapping("/kyc/pending")
    public ResponseEntity<List<KycReview>> getPendingKyc() {
        return ResponseEntity.ok(kycRepository.findByStatus("PENDING"));
    }

    @PutMapping("/kyc/{id}/approve")
    public ResponseEntity<?> approveKyc(@PathVariable Long id) {
        KycReview review = kycRepository.findById(id).orElseThrow(() -> new RuntimeException("KYC Review not found"));
        review.setStatus("APPROVED");
        review.setCompletedAt(LocalDateTime.now());
        kycRepository.save(review);
        
        log.info("[COMPLIANCE-CTRL] KYC {} approved for user {}", id, review.getUserEmail());
        
        // Publish event
        String eventPayload = String.format("{\"userEmail\":\"%s\", \"status\":\"VERIFIED\"}", review.getUserEmail());
        kafkaTemplate.send("kyc.updated", eventPayload);
        
        return ResponseEntity.ok(Map.of("message", "KYC Approved"));
    }

    @PutMapping("/kyc/{id}/reject")
    public ResponseEntity<?> rejectKyc(@PathVariable Long id) {
        KycReview review = kycRepository.findById(id).orElseThrow(() -> new RuntimeException("KYC Review not found"));
        review.setStatus("REJECTED");
        review.setCompletedAt(LocalDateTime.now());
        kycRepository.save(review);
        
        log.info("[COMPLIANCE-CTRL] KYC {} rejected for user {}", id, review.getUserEmail());
        
        // Publish event
        String eventPayload = String.format("{\"userEmail\":\"%s\", \"status\":\"REJECTED\"}", review.getUserEmail());
        kafkaTemplate.send("kyc.updated", eventPayload);
        
        return ResponseEntity.ok(Map.of("message", "KYC Rejected"));
    }

    // --- SAR Endpoints ---

    @GetMapping("/sar")
    public ResponseEntity<List<SuspiciousActivityReport>> getAllSars() {
        return ResponseEntity.ok(sarRepository.findAll());
    }

    @PostMapping("/sar")
    public ResponseEntity<SuspiciousActivityReport> createSar(@RequestBody SuspiciousActivityReport sar) {
        sar.setStatus("FILED");
        return ResponseEntity.ok(sarRepository.save(sar));
    }

    // --- CTR Endpoints ---

    @GetMapping("/ctr")
    public ResponseEntity<List<CurrencyTransactionReport>> getAllCtrs() {
        return ResponseEntity.ok(ctrRepository.findAll());
    }

    // --- Audit Trail Endpoints ---

    @GetMapping("/audit-trail")
    public ResponseEntity<List<AuditTrail>> getAuditTrails() {
        return ResponseEntity.ok(auditRepository.findAll());
    }
}
