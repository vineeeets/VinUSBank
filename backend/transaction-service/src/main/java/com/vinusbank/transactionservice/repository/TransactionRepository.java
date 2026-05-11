package com.vinusbank.transactionservice.repository;

import com.vinusbank.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Optional<Transaction> findByIdAndCustomerEmail(String id, String customerEmail);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);
}
