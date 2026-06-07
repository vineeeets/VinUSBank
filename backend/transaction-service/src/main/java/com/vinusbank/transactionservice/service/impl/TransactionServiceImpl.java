package com.vinusbank.transactionservice.service.impl;

import com.vinusbank.transactionservice.client.AccountServiceClient;
import com.vinusbank.transactionservice.dto.TransactionResponse;
import com.vinusbank.transactionservice.dto.TransferRequest;
import com.vinusbank.transactionservice.entity.Transaction;
import com.vinusbank.transactionservice.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionServiceImpl {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final AtomicInteger refCounter = new AtomicInteger(1);

    @Transactional
    public TransactionResponse transfer(String customerEmail, TransferRequest request) {
        log.info("[TRANSFER] ▶ Transfer initiated | User: {} | From: {} | To: {} | Amount: ${}",
                customerEmail, request.getSourceAccountId(), request.getDestinationAccountNumber(), request.getAmount());

        // 1. Resolve destination account by account number
        Map<String, Object> destAccount;
        try {
            log.debug("[TRANSFER] Resolving destination account number: {}", request.getDestinationAccountNumber());
            destAccount = accountServiceClient.getAccountByNumber(request.getDestinationAccountNumber());
        } catch (Exception e) {
            log.warn("[TRANSFER] ✗ Destination account not found: {} | Error: {}",
                    request.getDestinationAccountNumber(), e.getMessage());
            throw new RuntimeException("Destination account not found: " + request.getDestinationAccountNumber());
        }

        String destinationAccountId = (String) destAccount.get("id");
        String destinationAccountNumber = (String) destAccount.get("accountNumber");
        log.debug("[TRANSFER] Destination resolved | ID: {} | Number: {}", destinationAccountId, destinationAccountNumber);

        // 2. Resolve source account
        Map<String, Object> sourceAccount;
        try {
            log.debug("[TRANSFER] Resolving source account ID: {}", request.getSourceAccountId());
            sourceAccount = accountServiceClient.getAccountById(request.getSourceAccountId());
        } catch (Exception e) {
            log.warn("[TRANSFER] ✗ Source account not found | ID: {} | Error: {}",
                    request.getSourceAccountId(), e.getMessage());
            throw new RuntimeException("Source account not found");
        }

        // 3. Verify ownership
        String sourceOwner = (String) sourceAccount.get("customerEmail");
        if (!customerEmail.equals(sourceOwner)) {
            log.warn("[TRANSFER] ✗ SECURITY — Unauthorized transfer attempt | User: {} tried to use account owned by: {}",
                    customerEmail, sourceOwner);
            throw new RuntimeException("Access denied: you do not own this account");
        }

        String sourceAccountNumber = (String) sourceAccount.get("accountNumber");
        log.debug("[TRANSFER] Ownership verified | Source: {} | Owner: {}", sourceAccountNumber, customerEmail);

        // 4. Debit source
        try {
            log.info("[TRANSFER] Debiting source | AccountID: {} | Amount: ${}", request.getSourceAccountId(), request.getAmount());
            accountServiceClient.debitAccount(request.getSourceAccountId(), request.getAmount());
            log.info("[TRANSFER] ✓ Debit successful | AccountID: {}", request.getSourceAccountId());
        } catch (Exception e) {
            log.error("[TRANSFER] ✗ Debit failed | AccountID: {} | Reason: {}", request.getSourceAccountId(), e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }

        // 5. Credit destination
        try {
            log.info("[TRANSFER] Crediting destination | AccountID: {} | Amount: ${}", destinationAccountId, request.getAmount());
            accountServiceClient.creditAccount(destinationAccountId, request.getAmount());
            log.info("[TRANSFER] ✓ Credit successful | AccountID: {}", destinationAccountId);
        } catch (Exception e) {
            log.error("[TRANSFER] ✗ Credit failed — rolling back debit on source: {} | Reason: {}",
                    request.getSourceAccountId(), e.getMessage());
            accountServiceClient.creditAccount(request.getSourceAccountId(), request.getAmount());
            log.info("[TRANSFER] ↩ Rollback credit applied to source account: {}", request.getSourceAccountId());
            throw new RuntimeException("Credit failed, transfer rolled back: " + e.getMessage());
        }

        // 6. Save record
        String refNum = generateReferenceNumber();
        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .referenceNumber(refNum)
                .transactionType(Transaction.TransactionType.INTERNAL_TRANSFER)
                .customerEmail(customerEmail)
                .sourceAccountId(request.getSourceAccountId())
                .destinationAccountId(destinationAccountId)
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(request.getAmount())
                .feeAmount(BigDecimal.ZERO)
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.COMPLETED)
                .initiatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);
        log.info("[TRANSFER] ✓ Transfer complete | Ref: {} | From: {} → To: {} | Amount: ${}",
                refNum, sourceAccountNumber, destinationAccountNumber, request.getAmount());

        // Publish event
        try {
            String destEmail = (String) destAccount.get("customerEmail");
            String eventPayload = String.format("{\"sourceAccountEmail\":\"%s\", \"destinationAccountEmail\":\"%s\", \"amount\":%s, \"referenceNumber\":\"%s\", \"description\":\"%s\"}",
                    customerEmail, destEmail, request.getAmount(), refNum, request.getDescription());
            kafkaTemplate.send("txn.completed", eventPayload);
            log.info("[TRANSFER] Published txn.completed event for Ref: {}", refNum);
        } catch (Exception e) {
            log.error("[TRANSFER] Failed to publish txn.completed event", e);
        }

        return TransactionResponse.from(txn);
    }

    public List<TransactionResponse> getHistory(String customerEmail) {
        log.debug("[TRANSFER] Fetching history for user: {}", customerEmail);
        List<TransactionResponse> history = transactionRepository
                .findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        log.info("[TRANSFER] Returned {} transaction(s) for user: {}", history.size(), customerEmail);
        return history;
    }

    public TransactionResponse getById(String id, String customerEmail) {
        log.debug("[TRANSFER] Fetch by ID: {} | User: {}", id, customerEmail);
        Transaction txn = transactionRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[TRANSFER] ✗ Transaction not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Transaction not found");
                });
        return TransactionResponse.from(txn);
    }

    private String generateReferenceNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = refCounter.getAndIncrement();
        return "VUB" + date + String.format("%06d", seq);
    }
}
