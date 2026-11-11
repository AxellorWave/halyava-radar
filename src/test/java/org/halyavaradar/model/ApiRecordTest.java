package org.halyavaradar.model;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.source.cheapshark.CheapSharkData;
import org.halyavaradar.source.game.OfferType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRecordTest {
    private final JsonMapper jsonMapper = JsonMapperFactory.create();

    @Test
    void shouldPreserveDecimalPricesThroughDataAndJson() {
        BigDecimal salePrice = new BigDecimal("0.1234567890123456789");
        BigDecimal retailPrice = new BigDecimal("19.9900");
        CheapSharkData offer = new CheapSharkData(
                "deal-123",
                "Example Game",
                salePrice,
                retailPrice,
                "USD",
                OfferType.DISCOUNT,
                "12345",
                "7",
                "GOG",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        JsonNode data = jsonMapper.valueToTree(offer);
        ApiRecord original = new ApiRecord(
                "cheapshark",
                offer.title(),
                "https://www.cheapshark.com/redirect?dealID=deal-123",
                Instant.parse("2026-11-11T12:00:00Z"),
                data
        );

        String json = jsonMapper.writeValueAsString(original);
        ApiRecord restored = jsonMapper.readValue(json, ApiRecord.class);
        CheapSharkData restoredOffer = jsonMapper.treeToValue(restored.data(), CheapSharkData.class);

        assertAll(
                () -> assertEquals(0, salePrice.compareTo(data.get("salePrice").asDecimal()),
                        "Converting to JsonNode must preserve the price"),
                () -> assertTrue(restored.data().get("salePrice").isNumber()),
                () -> assertEquals(0, salePrice.compareTo(restored.data().get("salePrice").asDecimal()),
                        "Writing and reading JSON must preserve the price"),
                () -> assertEquals(0, salePrice.compareTo(restoredOffer.salePrice())),
                () -> assertEquals(0, retailPrice.compareTo(restoredOffer.retailPrice()))
        );
    }

    @Test
    void shouldPreserveGameRecordWhenWritingAndReadingJson() {
        JsonNode data = jsonMapper.readTree("""
                {
                  "dealId": "deal-123%3D",
                  "salePrice": "1.99",
                  "retailPrice": "19.99",
                  "currency": "USD",
                  "type": "DISCOUNT"
                }
                """);
        ApiRecord original = new ApiRecord(
                "cheapshark",
                "Example Game",
                "https://www.cheapshark.com/redirect?dealID=deal-123%3D",
                Instant.parse("2026-11-11T12:00:00.123456789Z"),
                data
        );

        String json = jsonMapper.writeValueAsString(original);
        ApiRecord restored = jsonMapper.readValue(json, ApiRecord.class);

        assertEquals(original, restored);
        assertTrue(jsonMapper.readTree(json).get("data").isObject());
        assertEquals("1.99", restored.data().get("salePrice").asString());
    }

    @Test
    void shouldPreserveWeatherRecordWithNestedDataWhenWritingAndReadingJson() {
        JsonNode data = jsonMapper.readTree("""
                {
                  "location": {
                    "name": "Санкт-Петербург",
                    "coordinates": {
                      "latitude": 59.9375,
                      "longitude": 30.3086
                    }
                  },
                  "forecast": [
                    {"date": "2026-11-11", "temperature": 5.5},
                    {"date": "2026-11-12", "temperature": 3.0}
                  ],
                  "alerts": [],
                  "description": null
                }
                """);
        ApiRecord original = new ApiRecord(
                "weather",
                "Погода в Санкт-Петербурге",
                null,
                Instant.parse("2026-11-11T13:00:00Z"),
                data
        );

        String json = jsonMapper.writeValueAsString(original);
        ApiRecord restored = jsonMapper.readValue(json, ApiRecord.class);

        assertEquals(original, restored);
        assertEquals("Санкт-Петербург", restored.data().get("location").get("name").asString());
        assertTrue(restored.data().get("forecast").isArray());
        assertEquals(2, restored.data().get("forecast").size());
    }
}
