package com.vinusbank.cardservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CardRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;
}
