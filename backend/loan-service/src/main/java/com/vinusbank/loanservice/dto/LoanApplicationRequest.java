package com.vinusbank.loanservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotBlank(message = "Account ID is required")
    private String accountId;

    @NotNull(message = "Loan type is required")
    private String loanType; // PERSONAL, HOME, AUTO, EDUCATION, BUSINESS

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "500.00", message = "Minimum loan amount is $500")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 3, message = "Minimum tenure is 3 months")
    private Integer tenureMonths;

    private String purpose;
}
