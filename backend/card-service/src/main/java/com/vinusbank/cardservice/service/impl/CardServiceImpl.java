package com.vinusbank.cardservice.service.impl;

import com.vinusbank.cardservice.dto.CardRequest;
import com.vinusbank.cardservice.dto.CardResponse;
import com.vinusbank.cardservice.entity.Card;
import com.vinusbank.cardservice.repository.CardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CardServiceImpl {

    @Autowired
    private CardRepository cardRepository;

    @Transactional
    public CardResponse requestCard(String customerEmail, CardRequest request) {
        log.info("[CARD] ▶ Card request | User: {} | Account: {} | Cardholder: {}",
                customerEmail, request.getAccountId(), request.getCardholderName());

        String lastFour = String.format("%04d", new Random().nextInt(10000));
        String masked = "****-****-****-" + lastFour;
        LocalDateTime now = LocalDateTime.now();
        int expiryYear = now.getYear() + 3;

        Card card = Card.builder()
                .id(UUID.randomUUID().toString())
                .customerEmail(customerEmail)
                .accountId(request.getAccountId())
                .cardNumberLastFour(lastFour)
                .cardNumberMasked(masked)
                .cardholderName(request.getCardholderName().toUpperCase())
                .cardType(Card.CardType.VIRTUAL_DEBIT)
                .expiryMonth(now.getMonthValue())
                .expiryYear(expiryYear)
                .status(Card.CardStatus.PENDING)
                .build();

        cardRepository.save(card);
        log.info("[CARD] ✓ Card issued | ID: {} | Masked: {} | Expiry: {}/{} | Status: PENDING",
                card.getId(), masked, now.getMonthValue(), expiryYear);
        return CardResponse.from(card);
    }

    public List<CardResponse> getMyCards(String customerEmail) {
        log.debug("[CARD] Fetching cards for user: {}", customerEmail);
        List<CardResponse> cards = cardRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
        log.info("[CARD] Found {} card(s) for user: {}", cards.size(), customerEmail);
        return cards;
    }

    public CardResponse getCardById(String id, String customerEmail) {
        log.debug("[CARD] Fetch by ID: {} | User: {}", id, customerEmail);
        Card card = cardRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[CARD] ✗ Card not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Card not found");
                });
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse activateCard(String id, String customerEmail) {
        log.info("[CARD] ▶ Activate request | CardID: {} | User: {}", id, customerEmail);
        Card card = cardRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[CARD] ✗ Activate failed — card not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Card not found");
                });

        if (card.getStatus() != Card.CardStatus.PENDING) {
            log.warn("[CARD] ✗ Activate rejected — card {} is in state: {} (must be PENDING)", id, card.getStatus());
            throw new RuntimeException("Card is not in PENDING state");
        }

        card.setStatus(Card.CardStatus.ACTIVE);
        card.setActivatedAt(LocalDateTime.now());
        cardRepository.save(card);
        log.info("[CARD] ✓ Card activated | ID: {} | Masked: {} | User: {}", id, card.getCardNumberMasked(), customerEmail);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse blockCard(String id, String customerEmail, String reason) {
        log.info("[CARD] ▶ Block request | CardID: {} | User: {} | Reason: {}", id, customerEmail, reason);
        Card card = cardRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[CARD] ✗ Block failed — card not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Card not found");
                });

        String blockReason = reason != null ? reason : "Blocked by cardholder";
        card.setStatus(Card.CardStatus.BLOCKED);
        card.setBlockedAt(LocalDateTime.now());
        card.setBlockReason(blockReason);
        cardRepository.save(card);
        log.info("[CARD] ✓ Card blocked | ID: {} | Masked: {} | Reason: {}", id, card.getCardNumberMasked(), blockReason);
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse unblockCard(String id, String customerEmail) {
        log.info("[CARD] ▶ Unblock request | CardID: {} | User: {}", id, customerEmail);
        Card card = cardRepository.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> {
                    log.warn("[CARD] ✗ Unblock failed — card not found | ID: {} | User: {}", id, customerEmail);
                    return new RuntimeException("Card not found");
                });

        if (card.getStatus() != Card.CardStatus.BLOCKED) {
            log.warn("[CARD] ✗ Unblock rejected — card {} is in state: {} (must be BLOCKED)", id, card.getStatus());
            throw new RuntimeException("Card is not blocked");
        }

        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBlockedAt(null);
        card.setBlockReason(null);
        cardRepository.save(card);
        log.info("[CARD] ✓ Card unblocked | ID: {} | Masked: {} | User: {}", id, card.getCardNumberMasked(), customerEmail);
        return CardResponse.from(card);
    }
}
