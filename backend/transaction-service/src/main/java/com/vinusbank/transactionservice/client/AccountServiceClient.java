package com.vinusbank.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feign client that calls account-service internal endpoints
 * to debit/credit account balances during transactions.
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PostMapping("/internal/accounts/{accountId}/debit")
    Map<String, Object> debitAccount(
            @PathVariable("accountId") String accountId,
            @RequestParam("amount") BigDecimal amount);

    @PostMapping("/internal/accounts/{accountId}/credit")
    Map<String, Object> creditAccount(
            @PathVariable("accountId") String accountId,
            @RequestParam("amount") BigDecimal amount);

    @GetMapping("/internal/accounts/by-number/{accountNumber}")
    Map<String, Object> getAccountByNumber(@PathVariable("accountNumber") String accountNumber);

    @GetMapping("/internal/accounts/{accountId}")
    Map<String, Object> getAccountById(@PathVariable("accountId") String accountId);
}
