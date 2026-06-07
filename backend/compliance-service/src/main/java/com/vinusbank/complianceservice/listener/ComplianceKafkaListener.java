package com.vinusbank.complianceservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinusbank.complianceservice.entity.CurrencyTransactionReport;
import com.vinusbank.complianceservice.event.TransactionCompletedEvent;
import com.vinusbank.complianceservice.repository.CurrencyTransactionReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ComplianceKafkaListener {

    @Autowired
    private CurrencyTransactionReportRepository ctrRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "txn.completed", groupId = "compliance-group")
    public void handleTransactionCompleted(String message) {
        log.info("[COMPLIANCE-LISTENER] Received txn.completed event: {}", message);
        try {
            TransactionCompletedEvent event = objectMapper.readValue(message, TransactionCompletedEvent.class);
            
            // Rule: Auto-generate CTR if amount > $10,000
            BigDecimal threshold = new BigDecimal("10000");
            if (event.getAmount() != null && event.getAmount().compareTo(threshold) > 0) {
                log.info("[COMPLIANCE-LISTENER] Transaction {} exceeds $10k threshold. Generating CTR.", event.getReferenceNumber());
                
                CurrencyTransactionReport ctr = CurrencyTransactionReport.builder()
                        .ctrNumber("CTR-" + event.getReferenceNumber())
                        .userEmail(event.getSourceAccountEmail())
                        .amount(event.getAmount())
                        .transactionDate(LocalDateTime.now())
                        .status("PENDING")
                        .build();
                        
                ctrRepository.save(ctr);
                log.info("[COMPLIANCE-LISTENER] CTR saved successfully.");
            }
        } catch (Exception e) {
            log.error("[COMPLIANCE-LISTENER] Error processing txn.completed event", e);
        }
    }
}
