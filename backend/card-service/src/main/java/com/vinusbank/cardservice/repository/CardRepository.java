package com.vinusbank.cardservice.repository;

import com.vinusbank.cardservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    Optional<Card> findByIdAndCustomerEmail(String id, String customerEmail);
}
