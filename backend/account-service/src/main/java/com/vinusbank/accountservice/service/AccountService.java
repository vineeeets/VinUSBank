package com.vinusbank.accountservice.service;

import com.vinusbank.accountservice.dto.AccountResponse;
import com.vinusbank.accountservice.dto.OpenAccountRequest;
import com.vinusbank.accountservice.entity.Account;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse openAccount(String customerEmail, OpenAccountRequest request);

    List<AccountResponse> getMyAccounts(String customerEmail);

    AccountResponse getAccountById(String id, String customerEmail);

    AccountResponse getBalance(String id, String customerEmail);

    void closeAccount(String id, String customerEmail);

    // Internal methods (called by other services, not exposed via Gateway)
    Account getAccountEntityById(String accountId);

    Account debitAccount(String accountId, BigDecimal amount);

    Account creditAccount(String accountId, BigDecimal amount);

    Account getAccountByNumber(String accountNumber);
}
