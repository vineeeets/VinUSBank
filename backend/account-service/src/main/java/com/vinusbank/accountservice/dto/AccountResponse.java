package com.vinusbank.accountservice.dto;

import com.vinusbank.accountservice.entity.Account;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String accountType;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal currentBalance;
    private BigDecimal holdAmount;
    private BigDecimal dailyTransferLimit;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime createdAt;

    public static AccountResponse from(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAccountType(account.getAccountType().name());
        dto.setCurrency(account.getCurrency());
        dto.setAvailableBalance(account.getAvailableBalance());
        dto.setCurrentBalance(account.getCurrentBalance());
        dto.setHoldAmount(account.getHoldAmount());
        dto.setDailyTransferLimit(account.getDailyTransferLimit());
        dto.setStatus(account.getStatus().name());
        dto.setOpenedAt(account.getOpenedAt());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }
}
