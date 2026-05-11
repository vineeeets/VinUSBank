package com.vinusbank.cardservice.dto;

import com.vinusbank.cardservice.entity.Card;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CardResponse {
    private String id;
    private String cardNumberMasked;
    private String cardNumberLastFour;
    private String cardholderName;
    private String cardType;
    private Integer expiryMonth;
    private Integer expiryYear;
    private BigDecimal dailySpendLimit;
    private Boolean onlineEnabled;
    private Boolean internationalEnabled;
    private String status;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;

    public static CardResponse from(Card card) {
        CardResponse dto = new CardResponse();
        dto.setId(card.getId());
        dto.setCardNumberMasked(card.getCardNumberMasked());
        dto.setCardNumberLastFour(card.getCardNumberLastFour());
        dto.setCardholderName(card.getCardholderName());
        dto.setCardType(card.getCardType().name());
        dto.setExpiryMonth(card.getExpiryMonth());
        dto.setExpiryYear(card.getExpiryYear());
        dto.setDailySpendLimit(card.getDailySpendLimit());
        dto.setOnlineEnabled(card.getOnlineEnabled());
        dto.setInternationalEnabled(card.getInternationalEnabled());
        dto.setStatus(card.getStatus().name());
        dto.setActivatedAt(card.getActivatedAt());
        dto.setCreatedAt(card.getCreatedAt());
        return dto;
    }
}
