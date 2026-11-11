package org.halyavaradar.source.freetogame;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.http.ApiRequestException;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.game.OfferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FreeToGameSourceTest {
    private static final URI GAMES_URI = URI.create(
            "https://www.freetogame.com/api/games?platform=pc&category=shooter&sort-by=release-date");
    private static final URI FIRST_GAME_URI = URI.create("https://www.freetogame.com/api/game?id=655");
    private static final URI SECOND_GAME_URI = URI.create("https://www.freetogame.com/api/game?id=656");
    private static final Map<String, String> HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");
    private static final Instant FETCHED_AT = Instant.parse("2026-11-11T12:00:00Z");

    private final JsonMapper jsonMapper = JsonMapperFactory.create();
    private final Clock clock = Clock.fixed(FETCHED_AT, ZoneOffset.UTC);
    private ApiHttpClient httpClient;
    private FreeToGameMapper mapper;
    private FreeToGameSource source;

    @BeforeEach
    void setUp() {
        httpClient = mock(ApiHttpClient.class);
        mapper = new FreeToGameMapper(jsonMapper);
        source = new FreeToGameSource(httpClient, mapper, jsonMapper, GAMES_URI, 2, clock);
    }

    @Test
    void shouldFetchUniqueGamesInOrderWithinLimitAndBuildRecords() throws IOException {
        when(httpClient.get(GAMES_URI, HEADERS)).thenReturn(readFixture("games.json"));
        when(httpClient.get(FIRST_GAME_URI, HEADERS)).thenReturn(readFixture("detail-full.json"));
        when(httpClient.get(SECOND_GAME_URI, HEADERS)).thenReturn(readFixture("detail-minimal.json"));

        List<ApiRecord> records = source.fetch();

        assertEquals("freetogame", source.getName());
        assertEquals(2, records.size());
        assertEquals(List.of("Example Shooter", "Example Browser Game"),
                records.stream().map(ApiRecord::title).toList());
        assertEquals(List.of(655, 656),
                records.stream().map(record -> record.data().get("gameId").asInt()).toList());
        for (ApiRecord record : records) {
            assertEquals("freetogame", record.source());
            assertEquals(FETCHED_AT, record.fetchedAt());
            assertEquals(record.data().get("profileUrl").asString(), record.url());
            assertEquals("FREE_TO_PLAY", record.data().get("type").asString());
        }
        assertEquals("https://www.freetogame.com/example-shooter", records.get(0).url());
        assertTrue(records.get(0).data().get("minimumSystemRequirements").isObject());
        assertEquals("8 GB RAM", records.get(0).data().get("minimumSystemRequirements").get("memory").asString());
        assertTrue(records.get(0).data().get("screenshotUrls").isArray());
        assertEquals(2, records.get(0).data().get("screenshotUrls").size());
        FreeToGameData restoredData = jsonMapper.treeToValue(records.get(0).data(), FreeToGameData.class);
        assertEquals(OfferType.FREE_TO_PLAY, restoredData.type());
        assertEquals("https://www.freetogame.com/open/example-shooter", restoredData.gameUrl());

        InOrder order = inOrder(httpClient);
        order.verify(httpClient).get(GAMES_URI, HEADERS);
        order.verify(httpClient).get(FIRST_GAME_URI, HEADERS);
        order.verify(httpClient).get(SECOND_GAME_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldReturnEmptyListWithoutRequestingDetailsForEmptyList() {
        when(httpClient.get(GAMES_URI, HEADERS)).thenReturn("[]");

        assertTrue(source.fetch().isEmpty());

        verify(httpClient).get(GAMES_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldFetchFewerGamesWhenListIsShorterThanLimit() throws IOException {
        source = new FreeToGameSource(httpClient, mapper, jsonMapper, GAMES_URI, 5, clock);
        when(httpClient.get(GAMES_URI, HEADERS)).thenReturn("[{\"id\":655}]");
        when(httpClient.get(FIRST_GAME_URI, HEADERS)).thenReturn(readFixture("detail-full.json"));

        assertEquals(1, source.fetch().size());

        verify(httpClient).get(GAMES_URI, HEADERS);
        verify(httpClient).get(FIRST_GAME_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveGameLimit(int limit) {
        assertThrows(IllegalArgumentException.class,
                () -> new FreeToGameSource(httpClient, mapper, jsonMapper, GAMES_URI, limit, clock));

        verifyNoInteractions(httpClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void shouldPropagateHttpFailureAndStopFetching(int failingRequest) throws IOException {
        List<URI> requests = List.of(GAMES_URI, FIRST_GAME_URI, SECOND_GAME_URI);
        List<String> responses = List.of(readFixture("games.json"), readFixture("detail-full.json"));
        for (int i = 0; i < failingRequest; i++) {
            when(httpClient.get(requests.get(i), HEADERS)).thenReturn(responses.get(i));
        }
        ApiRequestException failure = new ApiRequestException("HTTP request failed", new IOException("Connection failed"));
        when(httpClient.get(requests.get(failingRequest), HEADERS)).thenThrow(failure);

        assertSame(failure, assertThrows(ApiRequestException.class, source::fetch));

        InOrder order = inOrder(httpClient);
        for (int i = 0; i <= failingRequest; i++) {
            order.verify(httpClient).get(requests.get(i), HEADERS);
        }
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldPreserveInterruptionAndAvoidDetailRequest() {
        when(httpClient.get(GAMES_URI, HEADERS)).thenReturn("[{\"id\":655}]");

        Thread.currentThread().interrupt();
        try {
            ApiRequestException failure = assertThrows(ApiRequestException.class, source::fetch);

            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(httpClient).get(GAMES_URI, HEADERS);
            verifyNoMoreInteractions(httpClient);
        } finally {
            Thread.interrupted();
        }
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/freetogame/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
