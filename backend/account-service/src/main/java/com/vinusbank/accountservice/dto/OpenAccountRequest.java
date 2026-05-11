package com.vinusbank.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenAccountRequest {

    @NotNull(message = "Account type is required")
    private String accountType; // CHECKING, SAVINGS, MONEY_MARKET, CD

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    private String currency = "USD";
}
