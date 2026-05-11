package com.vinusbank.accountservice.service.impl;

import com.vinusbank.accountservice.dto.AccountResponse;
import com.vinusbank.accountservice.dto.OpenAccountRequest;
import com.vinusbank.accountservice.entity.Account;
import com.vinusbank.accountservice.repository.AccountRepository;
import com.vinusbank.accountservice.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    private static final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    @Transactional
    public AccountResponse openAccount(String customerEmail, OpenAccountRequest request) {
        log.info("[ACCOUNT] ▶ Opening new account | User: {} | Type: {} | InitialDeposit: ${} | Currency: {}",
                customerEmail, request.getAccountType(), request.getInitialDeposit(), request.getCurrency());

        Account.AccountType type;
        try {
            type = Account.AccountType.valueOf(request.getAccountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[ACCOUNT] ✗ Invalid account type provided: '{}' by user: {}", request.getAccountType(), customerEmail);
            throw new RuntimeException("Invalid account type: " + request.getAccountType());
        }

        String accountNumber = generateAccountNumber();
        BigDecimal deposit = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .customerEmail(customerEmail)
                .accountNumber(accountNumber)
                .accountType(type)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .availableBalance(deposit)
                .currentBalance(deposit)
                .holdAmount(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .openedAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);

        log.info("[ACCOUNT] ✓ Account opened successfully | ID: {} | Number: {} | Balance: ${}",
                account.getId(), account.getAccountNumber(), account.getAvailableBalance());

        return AccountResponse.from(account);
    }

    @Override
    public List<AccountResponse> getMyAccounts(String customerEmail) {
        log.debug("[ACCOUNT] Fetching all accounts for user: {}", customerEmail);
        List<AccountResponse> accounts = accountRepository.findByCustomerEmail(customerEmail)
                .stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
        log.info("[ACCOUNT] Found {} account(s) for user: {}", accounts.size(), customerEmail);
        return accounts;
    }

    @Override
    public AccountResponse getAccountById(String id, String customerEmail) {
        log.debug("[ACCOUNT] Fetching account | ID: {} | User: {}", id, customerEmail);
        Account account = accountRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[ACCOUNT] ✗ Account not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Account not found");
                });
        return AccountResponse.from(account);
    }

    @Override
    public AccountResponse getBalance(String id, String customerEmail) {
        log.debug("[ACCOUNT] Balance check | ID: {} | User: {}", id, customerEmail);
        return getAccountById(id, customerEmail);
    }

    @Override
    @Transactional
    public void closeAccount(String id, String customerEmail) {
        log.info("[ACCOUNT] ▶ Close request | ID: {} | User: {}", id, customerEmail);
        Account account = accountRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[ACCOUNT] ✗ Close failed — account not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Account not found");
                });
        account.setStatus(Account.AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);
        log.info("[ACCOUNT] ✓ Account closed | Number: {} | User: {}", account.getAccountNumber(), customerEmail);
    }

    @Override
    public Account getAccountEntityById(String accountId) {
        log.debug("[ACCOUNT] Internal fetch | ID: {}", accountId);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("[ACCOUNT] ✗ Internal fetch failed — account not found: {}", accountId);
                    return new RuntimeException("Account not found: " + accountId);
                });
    }

    @Override
    @Transactional
    public Account debitAccount(String accountId, BigDecimal amount) {
        log.info("[ACCOUNT] ▶ DEBIT | AccountID: {} | Amount: ${}", accountId, amount);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("[ACCOUNT] ✗ Debit failed — account not found: {}", accountId);
                    return new RuntimeException("Account not found: " + accountId);
                });

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            log.warn("[ACCOUNT] ✗ Insufficient balance | Account: {} | Available: ${} | Requested: ${}",
                    accountId, account.getAvailableBalance(), amount);
            throw new RuntimeException("Insufficient balance");
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        Account saved = accountRepository.save(account);
        log.info("[ACCOUNT] ✓ Debit successful | Account: {} | Debited: ${} | New Balance: ${}",
                accountId, amount, saved.getAvailableBalance());
        return saved;
    }

    @Override
    @Transactional
    public Account creditAccount(String accountId, BigDecimal amount) {
        log.info("[ACCOUNT] ▶ CREDIT | AccountID: {} | Amount: ${}", accountId, amount);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.error("[ACCOUNT] ✗ Credit failed — account not found: {}", accountId);
                    return new RuntimeException("Account not found: " + accountId);
                });

        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        Account saved = accountRepository.save(account);
        log.info("[ACCOUNT] ✓ Credit successful | Account: {} | Credited: ${} | New Balance: ${}",
                accountId, amount, saved.getAvailableBalance());
        return saved;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        log.debug("[ACCOUNT] Lookup by number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("[ACCOUNT] ✗ Account not found by number: {}", accountNumber);
                    return new RuntimeException("Account not found: " + accountNumber);
                });
    }

    private String generateAccountNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = counter.getAndIncrement();
        return "VUS" + datePart + String.format("%04d", seq);
    }
}
