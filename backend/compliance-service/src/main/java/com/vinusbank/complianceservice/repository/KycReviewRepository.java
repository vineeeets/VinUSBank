package com.vinusbank.complianceservice.repository;

import com.vinusbank.complianceservice.entity.KycReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycReviewRepository extends JpaRepository<KycReview, Long> {
    List<KycReview> findByStatus(String status);
}
