package com.vinusbank.cardservice.controller;

import com.vinusbank.cardservice.dto.CardRequest;
import com.vinusbank.cardservice.dto.CardResponse;
import com.vinusbank.cardservice.service.impl.CardServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardServiceImpl cardService;

    @PostMapping
    public ResponseEntity<?> requestCard(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody CardRequest request) {
        log.info("[CARD-CTRL] POST /cards | User: {} | Account: {} | Cardholder: {}",
                email, request.getAccountId(), request.getCardholderName());
        try {
            CardResponse card = cardService.requestCard(email, request);
            log.info("[CARD-CTRL] ✓ Card issued | Masked: {} | User: {}", card.getCardNumberMasked(), email);
            return ResponseEntity.ok(Map.of("message", "Card requested successfully", "card", card));
        } catch (Exception e) {
            log.error("[CARD-CTRL] ✗ Card request failed | User: {} | Reason: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getMyCards(
            @RequestHeader("X-User-Email") String email) {
        log.info("[CARD-CTRL] GET /cards | User: {}", email);
        return ResponseEntity.ok(cardService.getMyCards(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCardById(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[CARD-CTRL] GET /cards/{} | User: {}", id, email);
        try {
            return ResponseEntity.ok(cardService.getCardById(id, email));
        } catch (Exception e) {
            log.warn("[CARD-CTRL] ✗ Card not found | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateCard(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[CARD-CTRL] POST /cards/{}/activate | User: {}", id, email);
        try {
            CardResponse card = cardService.activateCard(id, email);
            log.info("[CARD-CTRL] ✓ Activated | ID: {} | User: {}", id, email);
            return ResponseEntity.ok(Map.of("message", "Card activated successfully", "card", card));
        } catch (Exception e) {
            log.warn("[CARD-CTRL] ✗ Activate failed | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<?> blockCard(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id,
            @RequestParam(required = false) String reason) {
        log.info("[CARD-CTRL] POST /cards/{}/block | User: {} | Reason: {}", id, email, reason);
        try {
            CardResponse card = cardService.blockCard(id, email, reason);
            log.info("[CARD-CTRL] ✓ Blocked | ID: {} | User: {}", id, email);
            return ResponseEntity.ok(Map.of("message", "Card blocked", "card", card));
        } catch (Exception e) {
            log.warn("[CARD-CTRL] ✗ Block failed | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<?> unblockCard(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        log.info("[CARD-CTRL] POST /cards/{}/unblock | User: {}", id, email);
        try {
            CardResponse card = cardService.unblockCard(id, email);
            log.info("[CARD-CTRL] ✓ Unblocked | ID: {} | User: {}", id, email);
            return ResponseEntity.ok(Map.of("message", "Card unblocked", "card", card));
        } catch (Exception e) {
            log.warn("[CARD-CTRL] ✗ Unblock failed | ID: {} | User: {} | Reason: {}", id, email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
