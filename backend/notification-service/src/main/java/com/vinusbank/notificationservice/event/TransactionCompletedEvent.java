package com.vinusbank.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    private String sourceAccountEmail;
    private String destinationAccountEmail;
    private BigDecimal amount;
    private String referenceNumber;
    private String description;
}
