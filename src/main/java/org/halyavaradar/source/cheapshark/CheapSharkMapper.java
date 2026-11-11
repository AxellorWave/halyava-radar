package org.halyavaradar.source.cheapshark;

import org.halyavaradar.source.game.OfferType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CheapSharkMapper {
    private final JsonMapper jsonMapper;

    public CheapSharkMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<String> readDealIds(String json) {
        JsonNode node = jsonMapper.readTree(json);
        List<String> dealIds = new ArrayList<>();
        for (JsonNode dealNode : node) {
            JsonNode dealIdNode = dealNode.get("dealID");
            String dealId = dealIdNode.asString();
            dealIds.add(dealId);
        }
        return dealIds;
    }

    public CheapSharkData toData(String dealId, String json, String storeName) {
        JsonNode node = jsonMapper.readTree(json);
        JsonNode gameInfoNode = node.get("gameInfo");
        JsonNode nameNode = gameInfoNode.get("name");
        BigDecimal salePrice = new BigDecimal(gameInfoNode.get("salePrice").asString());
        BigDecimal retailPrice = new BigDecimal(gameInfoNode.get("retailPrice").asString());
        OfferType offerType;
        if (salePrice.compareTo(retailPrice) == 0 && retailPrice.compareTo(BigDecimal.ZERO) == 0) {
            offerType = OfferType.FREE_TO_PLAY;
        } else if (retailPrice.compareTo(salePrice) > 0 && salePrice.compareTo(BigDecimal.ZERO) == 0) {
            offerType = OfferType.GIVEAWAY;
        } else {
            offerType = OfferType.DISCOUNT;
        }

        String gameId = gameInfoNode.path("gameID").asString(null);
        String storeId = gameInfoNode.path("storeID").asString(null);
        String thumbnailUrl = gameInfoNode.path("thumb").asString(null);
        String steamAppId = gameInfoNode.path("steamAppID").asString(null);
        String steamRatingText = gameInfoNode.path("steamRatingText").asString(null);
        String steamRatingPercent = gameInfoNode.path("steamRatingPercent").asString(null);
        String steamReviewCount = gameInfoNode.path("steamRatingCount").asString(null);
        String metacriticScore = gameInfoNode.path("metacriticScore").asString(null);
        String publisher = gameInfoNode.path("publisher").asString(null);
        if ("N/A".equalsIgnoreCase(publisher)) {
            publisher = null;
        }

        JsonNode releaseDateNode = gameInfoNode.get("releaseDate");
        Instant releaseDate = null;
        if (releaseDateNode != null && !releaseDateNode.isNull()) {
            long releaseDateSeconds = releaseDateNode.asLong();
            if (releaseDateSeconds > 0) {
                releaseDate = Instant.ofEpochSecond(releaseDateSeconds);
            }
        }

        return new CheapSharkData(
                dealId,
                nameNode.asString(),
                salePrice,
                retailPrice,
                "USD",
                offerType,
                gameId,
                storeId,
                storeName,
                thumbnailUrl,
                steamAppId,
                steamRatingText,
                steamRatingPercent == null ? null : Integer.parseInt(steamRatingPercent),
                steamReviewCount == null ? null : Integer.parseInt(steamReviewCount),
                metacriticScore == null ? null : Integer.parseInt(metacriticScore),
                releaseDate,
                publisher
        );
    }
}
