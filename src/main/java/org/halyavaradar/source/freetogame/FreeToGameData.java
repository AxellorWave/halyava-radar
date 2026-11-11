package org.halyavaradar.source.freetogame;

import org.halyavaradar.source.game.OfferType;

import java.time.LocalDate;
import java.util.List;

public record FreeToGameData(
        int gameId,
        String title,
        String thumbnailUrl,
        String status,
        String shortDescription,
        String description,
        String gameUrl,
        String genre,
        String platform,
        String publisher,
        String developer,
        LocalDate releaseDate,
        String profileUrl,
        OfferType type,
        FreeToGameRequirements minimumSystemRequirements,
        List<String> screenshotUrls
) {
}
