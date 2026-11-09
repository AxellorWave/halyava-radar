package org.halyavaradar.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GameOfferTest {
    @Test
    void shouldStoreAllFields() {
        GameOffer offer = new GameOffer(
                "cheapshark",
                "deal-123",
                "Example Game",
                OfferType.DISCOUNT,
                "https://example.com/offer",
                new BigDecimal("99.9"),
                "USD"
        );

        assertEquals("cheapshark", offer.getSource());
        assertEquals("deal-123", offer.getSourceId());
        assertEquals("Example Game", offer.getTitle());
        assertEquals(OfferType.DISCOUNT, offer.getType());
        assertEquals("https://example.com/offer", offer.getUrl());
        assertEquals(new BigDecimal("99.9"), offer.getPrice());
        assertEquals("USD", offer.getCurrency());
    }

    @Test
    void shouldAllowUnknownPriceAndCurrency() {
        GameOffer offer = new GameOffer(
                "cheapshark",
                "deal-123",
                "Example Game",
                OfferType.DISCOUNT,
                "https://example.com/offer",
                null,
                null
        );

        assertNull(offer.getPrice());
        assertNull(offer.getCurrency());
    }
}
