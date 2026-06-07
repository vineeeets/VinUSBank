package com.vinusbank.complianceservice.repository;

import com.vinusbank.complianceservice.entity.CurrencyTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyTransactionReportRepository extends JpaRepository<CurrencyTransactionReport, Long> {
}
