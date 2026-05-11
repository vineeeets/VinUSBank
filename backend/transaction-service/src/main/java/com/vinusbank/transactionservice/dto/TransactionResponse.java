package com.vinusbank.transactionservice.dto;

import com.vinusbank.transactionservice.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private String id;
    private String referenceNumber;
    private String transactionType;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private String description;
    private String status;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    public static TransactionResponse from(Transaction txn) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(txn.getId());
        dto.setReferenceNumber(txn.getReferenceNumber());
        dto.setTransactionType(txn.getTransactionType().name());
        dto.setSourceAccountNumber(txn.getSourceAccountNumber());
        dto.setDestinationAccountNumber(txn.getDestinationAccountNumber());
        dto.setAmount(txn.getAmount());
        dto.setFeeAmount(txn.getFeeAmount());
        dto.setDescription(txn.getDescription());
        dto.setStatus(txn.getStatus().name());
        dto.setInitiatedAt(txn.getInitiatedAt());
        dto.setCompletedAt(txn.getCompletedAt());
        return dto;
    }
}
