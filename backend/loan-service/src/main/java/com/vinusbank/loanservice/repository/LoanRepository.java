package com.vinusbank.loanservice.repository;

import com.vinusbank.loanservice.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {

    List<Loan> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Optional<Loan> findByIdAndCustomerEmail(String id, String customerEmail);

    Optional<Loan> findByLoanNumber(String loanNumber);
}
