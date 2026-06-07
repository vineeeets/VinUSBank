package com.vinusbank.complianceservice.repository;

import com.vinusbank.complianceservice.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
}
