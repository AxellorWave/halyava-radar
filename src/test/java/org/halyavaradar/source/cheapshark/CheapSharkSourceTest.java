package org.halyavaradar.source.cheapshark;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.http.ApiRequestException;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.game.OfferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CheapSharkSourceTest {
    private static final URI STORES_URI = URI.create("https://www.cheapshark.com/api/1.0/stores");
    private static final URI DISCOUNTS_URI = URI.create(
            "https://www.cheapshark.com/api/1.0/deals?storeID=7&upperPrice=10&pageSize=5&onSale=1");
    private static final URI FREE_DEALS_URI = URI.create(
            "https://www.cheapshark.com/api/1.0/deals?storeID=7&upperPrice=0&pageSize=5");
    private static final List<String> DEAL_IDS = List.of("first%2Bdeal%3D", "second%2Fdeal%3D", "third%3D");
    private static final Map<String, String> HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");
    private static final Instant FETCHED_AT = Instant.parse("2026-11-11T12:00:00Z");
    private static final String SINGLE_DEAL_JSON = "[{\"dealID\":\"first%2Bdeal%3D\"}]";

    private final JsonMapper jsonMapper = JsonMapperFactory.create();
    private ApiHttpClient httpClient;
    private CheapSharkSource source;

    @BeforeEach
    void setUp() {
        httpClient = mock(ApiHttpClient.class);
        source = new CheapSharkSource(
                httpClient,
                new CheapSharkMapper(jsonMapper),
                jsonMapper,
                DISCOUNTS_URI,
                FREE_DEALS_URI,
                Clock.fixed(FETCHED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldFetchUniqueDealsInOrderAndBuildRecords() throws IOException {
        when(httpClient.get(STORES_URI, HEADERS)).thenReturn(readFixture("stores.json"));
        when(httpClient.get(DISCOUNTS_URI, HEADERS)).thenReturn(readFixture("source-discounts.json"));
        when(httpClient.get(FREE_DEALS_URI, HEADERS)).thenReturn(readFixture("source-free-deals.json"));
        when(httpClient.get(detailUri(DEAL_IDS.get(0)), HEADERS)).thenReturn(readFixture("detail-full.json"));
        when(httpClient.get(detailUri(DEAL_IDS.get(1)), HEADERS)).thenReturn(readFixture("detail-giveaway.json"));
        when(httpClient.get(detailUri(DEAL_IDS.get(2)), HEADERS)).thenReturn(readFixture("detail-free-to-play.json"));

        List<ApiRecord> records = source.fetch();

        assertEquals("cheapshark", source.getName());
        assertEquals(3, records.size());
        assertEquals(List.of("Example Game", "Giveaway Game", "Free Game"),
                records.stream().map(ApiRecord::title).toList());
        assertEquals(DEAL_IDS, records.stream().map(record -> record.data().get("dealId").asString()).toList());
        for (int i = 0; i < records.size(); i++) {
            ApiRecord record = records.get(i);
            assertEquals("cheapshark", record.source());
            assertEquals(FETCHED_AT, record.fetchedAt());
            assertEquals("https://www.cheapshark.com/redirect?dealID=" + DEAL_IDS.get(i), record.url());
            assertTrue(record.data().isObject());
            assertEquals("GOG", record.data().get("storeName").asString());
            assertEquals("7", record.data().get("storeId").asString());
        }
        List<CheapSharkData> data = records.stream()
                .map(record -> jsonMapper.treeToValue(record.data(), CheapSharkData.class))
                .toList();
        assertEquals(List.of(OfferType.DISCOUNT, OfferType.GIVEAWAY, OfferType.FREE_TO_PLAY),
                data.stream().map(CheapSharkData::type).toList());
        assertEquals(0, new BigDecimal("1.9900").compareTo(data.get(0).salePrice()));
        assertEquals("https://example.com/game.jpg", data.get(0).thumbnailUrl());

        InOrder order = inOrder(httpClient);
        order.verify(httpClient).get(STORES_URI, HEADERS);
        order.verify(httpClient).get(DISCOUNTS_URI, HEADERS);
        order.verify(httpClient).get(FREE_DEALS_URI, HEADERS);
        for (String dealId : DEAL_IDS) {
            order.verify(httpClient).get(detailUri(dealId), HEADERS);
        }
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldReturnEmptyListWithoutRequestingDetailsWhenBothListsAreEmpty() {
        when(httpClient.get(STORES_URI, HEADERS)).thenReturn("[]");
        when(httpClient.get(DISCOUNTS_URI, HEADERS)).thenReturn("[]");
        when(httpClient.get(FREE_DEALS_URI, HEADERS)).thenReturn("[]");

        assertTrue(source.fetch().isEmpty());

        InOrder order = inOrder(httpClient);
        order.verify(httpClient).get(STORES_URI, HEADERS);
        order.verify(httpClient).get(DISCOUNTS_URI, HEADERS);
        order.verify(httpClient).get(FREE_DEALS_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFetchDealsWhenOnlyOneListIsEmpty(boolean discountsEmpty) throws IOException {
        when(httpClient.get(STORES_URI, HEADERS)).thenReturn(readFixture("stores.json"));
        when(httpClient.get(DISCOUNTS_URI, HEADERS)).thenReturn(discountsEmpty ? "[]" : SINGLE_DEAL_JSON);
        when(httpClient.get(FREE_DEALS_URI, HEADERS)).thenReturn(discountsEmpty ? SINGLE_DEAL_JSON : "[]");
        when(httpClient.get(detailUri(DEAL_IDS.get(0)), HEADERS)).thenReturn(readFixture("detail-full.json"));

        List<ApiRecord> records = source.fetch();

        assertEquals(1, records.size());
        assertEquals(DEAL_IDS.get(0), records.get(0).data().get("dealId").asString());
    }

    @ParameterizedTest
    @CsvSource({"19.99, 19.99", "25.00, 19.99"})
    void shouldSkipDealsThatAreNoLongerFreeOrDiscounted(String salePrice, String retailPrice) {
        String json = """
                {
                  "gameInfo": {
                    "name": "Example Game",
                    "salePrice": "%s",
                    "retailPrice": "%s"
                  }
                }
                """.formatted(salePrice, retailPrice);
        when(httpClient.get(STORES_URI, HEADERS)).thenReturn("[]");
        when(httpClient.get(DISCOUNTS_URI, HEADERS)).thenReturn(SINGLE_DEAL_JSON);
        when(httpClient.get(FREE_DEALS_URI, HEADERS)).thenReturn("[]");
        when(httpClient.get(detailUri(DEAL_IDS.get(0)), HEADERS)).thenReturn(json);

        assertTrue(source.fetch().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void shouldPropagateRequestFailureAndStopFetching(int failingRequest) throws IOException {
        List<URI> requests = List.of(STORES_URI, DISCOUNTS_URI, FREE_DEALS_URI,
                detailUri(DEAL_IDS.get(0)), detailUri(DEAL_IDS.get(1)));
        List<String> responses = List.of(readFixture("stores.json"), readFixture("source-discounts.json"),
                readFixture("source-free-deals.json"), readFixture("detail-full.json"));
        for (int i = 0; i < failingRequest; i++) {
            when(httpClient.get(requests.get(i), HEADERS)).thenReturn(responses.get(i));
        }
        ApiRequestException failure = new ApiRequestException("HTTP request failed", new IOException("Connection failed"));
        when(httpClient.get(requests.get(failingRequest), HEADERS)).thenThrow(failure);

        ApiRequestException result = assertThrows(ApiRequestException.class, source::fetch);

        assertSame(failure, result);
        InOrder order = inOrder(httpClient);
        for (int i = 0; i <= failingRequest; i++) {
            order.verify(httpClient).get(requests.get(i), HEADERS);
        }
        verifyNoMoreInteractions(httpClient);
    }

    private URI detailUri(String dealId) {
        return URI.create("https://www.cheapshark.com/api/1.0/deals?id=" + dealId);
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/cheapshark/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
