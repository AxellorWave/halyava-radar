package org.halyavaradar.source.cheapshark;

import org.halyavaradar.model.OfferType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheapSharkMapperTest {
    private final CheapSharkMapper mapper = new CheapSharkMapper(JsonMapper.builder().build());

    @Test
    void shouldReadDealIdsInOrderWithoutChangingEncoding() throws IOException {
        String json = readFixture("deals.json");

        List<String> ids = mapper.readDealIds(json);

        assertEquals(List.of("first%2Bdeal%3D", "second%2Fdeal%3D"), ids);
    }

    @Test
    void shouldReturnEmptyListForEmptyArray() {
        assertTrue(mapper.readDealIds("[]").isEmpty());
    }

    @Test
    void shouldMapAllFieldsOfDiscountedDeal() throws IOException {
        String json = readFixture("detail-full.json");

        CheapSharkData result = mapper.toData("deal%2B123%3D", json, "GOG");

        CheapSharkData expected = new CheapSharkData(
                "deal%2B123%3D",
                "Example Game",
                new BigDecimal("1.9900"),
                new BigDecimal("19.99"),
                "USD",
                OfferType.DISCOUNT,
                "12345",
                "7",
                "GOG",
                "https://example.com/game.jpg",
                "67890",
                "Very Positive",
                89,
                1094,
                85,
                Instant.parse("2025-01-01T00:00:00Z"),
                "Example Publisher"
        );
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
            "0.00, 19.99, GIVEAWAY",
            "0.00, 0.00, FREE_TO_PLAY",
            "0, 0.00, FREE_TO_PLAY"
    })
    void shouldClassifyFreeDeals(String salePrice, String retailPrice, OfferType expectedType) {
        String json = """
                {
                  "gameInfo": {
                    "name": "Example Game",
                    "salePrice": "%s",
                    "retailPrice": "%s"
                  }
                }
                """.formatted(salePrice, retailPrice);

        CheapSharkData result = mapper.toData("deal-123", json, "GOG");

        assertEquals(expectedType, result.type());
        assertEquals(new BigDecimal(salePrice), result.salePrice());
        assertEquals(new BigDecimal(retailPrice), result.retailPrice());
    }

    @ParameterizedTest
    @ValueSource(strings = {"detail-minimal.json", "detail-null.json"})
    void shouldPreserveMissingOptionalValuesAsNull(String fixture) throws IOException {
        CheapSharkData result = mapper.toData("deal-123", readFixture(fixture), null);

        CheapSharkData expected = new CheapSharkData(
                "deal-123",
                "Example Game",
                new BigDecimal("1.99"),
                new BigDecimal("19.99"),
                "USD",
                OfferType.DISCOUNT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"N/A", "n/a"})
    void shouldTreatUnknownPublisherAndZeroReleaseDateAsNull(String publisher) {
        String json = """
                {
                  "gameInfo": {
                    "name": "Example Game",
                    "salePrice": "1.99",
                    "retailPrice": "19.99",
                    "releaseDate": 0,
                    "publisher": "%s"
                  }
                }
                """.formatted(publisher);

        CheapSharkData result = mapper.toData("deal-123", json, "GOG");

        assertNull(result.publisher());
        assertNull(result.releaseDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "not json"})
    void shouldRejectMalformedJson(String json) {
        assertThrows(JacksonException.class, () -> mapper.readDealIds(json));
        assertThrows(JacksonException.class, () -> mapper.toData("deal-123", json, "GOG"));
    }

    @ParameterizedTest
    @CsvSource({
            "invalid, 19.99",
            "1.99, invalid"
    })
    void shouldRejectInvalidRequiredPrice(String salePrice, String retailPrice) {
        String json = """
                {
                  "gameInfo": {
                    "name": "Example Game",
                    "salePrice": "%s",
                    "retailPrice": "%s"
                  }
                }
                """.formatted(salePrice, retailPrice);

        assertThrows(NumberFormatException.class, () -> mapper.toData("deal-123", json, "GOG"));
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/cheapshark/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
