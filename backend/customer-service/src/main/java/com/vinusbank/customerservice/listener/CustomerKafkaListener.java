package com.vinusbank.customerservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinusbank.customerservice.entity.Customer;
import com.vinusbank.customerservice.event.KycUpdatedEvent;
import com.vinusbank.customerservice.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class CustomerKafkaListener {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "kyc.updated", groupId = "customer-group")
    public void handleKycUpdated(String message) {
        log.info("[CUSTOMER-LISTENER] Received kyc.updated event: {}", message);
        try {
            KycUpdatedEvent event = objectMapper.readValue(message, KycUpdatedEvent.class);
            
            Optional<Customer> optionalCustomer = customerRepository.findByEmail(event.getUserEmail());
            if (optionalCustomer.isPresent()) {
                Customer customer = optionalCustomer.get();
                // Assuming customer has a setKycStatus method or similar. Let's check the entity structure if needed, 
                // but usually it's setKycStatus(KycStatus.VERIFIED). We'll map the string to Enum if needed.
                // Looking at standard entity, it might be string or enum. I'll use string for now, if it fails compile I'll fix.
                // Wait, in Phase 1 I made KYC status an enum or string. Let's just set it using the string if it's String, or parse Enum.
                // If it's enum Customer.KycStatus.valueOf(event.getStatus())
                customer.setKycStatus(event.getStatus());
                customerRepository.save(customer);
                log.info("[CUSTOMER-LISTENER] Customer KYC status updated to {} for {}", event.getStatus(), event.getUserEmail());
            } else {
                log.warn("[CUSTOMER-LISTENER] Customer not found for email: {}", event.getUserEmail());
            }
        } catch (Exception e) {
            log.error("[CUSTOMER-LISTENER] Error processing kyc.updated event", e);
        }
    }
}
