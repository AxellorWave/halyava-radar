package org.halyavaradar.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRecordTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

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
