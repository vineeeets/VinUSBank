package com.vinusbank.complianceservice.repository;

import com.vinusbank.complianceservice.entity.SuspiciousActivityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuspiciousActivityReportRepository extends JpaRepository<SuspiciousActivityReport, Long> {
}
