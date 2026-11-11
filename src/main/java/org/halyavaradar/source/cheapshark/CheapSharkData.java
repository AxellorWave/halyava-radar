package org.halyavaradar.source.cheapshark;

import org.halyavaradar.source.game.OfferType;

import java.math.BigDecimal;
import java.time.Instant;

public record CheapSharkData(
        String dealId,
        String title,
        BigDecimal salePrice,
        BigDecimal retailPrice,
        String currency,
        OfferType type,
        String gameId,
        String storeId,
        String storeName,
        String thumbnailUrl,
        String steamAppId,
        String steamRatingText,
        Integer steamRatingPercent,
        Integer steamReviewCount,
        Integer metacriticScore,
        Instant releaseDate,
        String publisher
) {
}
