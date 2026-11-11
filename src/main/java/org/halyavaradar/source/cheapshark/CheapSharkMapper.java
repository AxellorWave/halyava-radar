package org.halyavaradar.source.cheapshark;

import org.halyavaradar.model.GameOffer;

import org.halyavaradar.model.OfferType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
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

    public GameOffer toOffer(String dealId, String json) {
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

        return new GameOffer(
                "cheapshark",
                dealId,
                nameNode.asString(),
                offerType,
                "https://www.cheapshark.com/redirect?dealID=" + dealId,
                salePrice,
                "USD"
        );
    }
}
