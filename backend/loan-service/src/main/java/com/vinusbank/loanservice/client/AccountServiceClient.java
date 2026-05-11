package com.vinusbank.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PostMapping("/internal/accounts/{accountId}/credit")
    Map<String, Object> creditAccount(
            @PathVariable("accountId") String accountId,
            @RequestParam("amount") BigDecimal amount);

    @GetMapping("/internal/accounts/{accountId}")
    Map<String, Object> getAccountById(@PathVariable("accountId") String accountId);
}
