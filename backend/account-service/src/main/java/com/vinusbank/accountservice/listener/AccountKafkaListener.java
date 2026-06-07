package com.vinusbank.accountservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinusbank.accountservice.entity.Account;
import com.vinusbank.accountservice.event.KycUpdatedEvent;
import com.vinusbank.accountservice.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AccountKafkaListener {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "kyc.updated", groupId = "account-group")
    public void handleKycUpdated(String message) {
        log.info("[ACCOUNT-LISTENER] Received kyc.updated event: {}", message);
        try {
            KycUpdatedEvent event = objectMapper.readValue(message, KycUpdatedEvent.class);
            
            if ("VERIFIED".equalsIgnoreCase(event.getStatus())) {
                List<Account> accounts = accountRepository.findByCustomerEmail(event.getUserEmail());
                boolean updated = false;
                for (Account account : accounts) {
                    if (account.getStatus() == Account.AccountStatus.PENDING) {
                        account.setStatus(Account.AccountStatus.ACTIVE);
                        updated = true;
                    }
                }
                if (updated) {
                    accountRepository.saveAll(accounts);
                    log.info("[ACCOUNT-LISTENER] Activated pending accounts for user: {}", event.getUserEmail());
                } else {
                    log.info("[ACCOUNT-LISTENER] No pending accounts to activate for user: {}", event.getUserEmail());
                }
            } else if ("REJECTED".equalsIgnoreCase(event.getStatus())) {
                List<Account> accounts = accountRepository.findByCustomerEmail(event.getUserEmail());
                boolean updated = false;
                for (Account account : accounts) {
                    if (account.getStatus() == Account.AccountStatus.PENDING) {
                        account.setStatus(Account.AccountStatus.SUSPENDED);
                        updated = true;
                    }
                }
                if (updated) {
                    accountRepository.saveAll(accounts);
                    log.info("[ACCOUNT-LISTENER] Suspended pending accounts for rejected KYC user: {}", event.getUserEmail());
                }
            }
        } catch (Exception e) {
            log.error("[ACCOUNT-LISTENER] Error processing kyc.updated event", e);
        }
    }
}
