package com.vinusbank.accountservice.repository;

import com.vinusbank.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByCustomerEmail(String customerEmail);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIdAndCustomerEmail(String id, String customerEmail);
}
