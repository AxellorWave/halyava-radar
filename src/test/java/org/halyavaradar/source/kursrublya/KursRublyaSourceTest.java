package org.halyavaradar.source.kursrublya;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.http.ApiRequestException;
import org.halyavaradar.model.ApiRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class KursRublyaSourceTest {
    private static final URI SERIES_URI = URI.create("https://kurs-rublya.ru/api/v1/rates/USD/series/?limit=2");
    private static final Map<String, String> HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");
    private static final Instant FETCHED_AT = Instant.parse("2026-11-12T12:00:00Z");

    private final JsonMapper jsonMapper = JsonMapperFactory.create();
    private final KursRublyaMapper mapper = new KursRublyaMapper(jsonMapper);
    private final Clock clock = Clock.fixed(FETCHED_AT, ZoneOffset.UTC);
    private ApiHttpClient httpClient;
    private KursRublyaSource source;

    @BeforeEach
    void setUp() {
        httpClient = mock(ApiHttpClient.class);
        source = new KursRublyaSource(httpClient, mapper, jsonMapper, SERIES_URI, clock);
    }

    @Test
    void shouldBuildOneRecordContainingEntireHistory() throws IOException {
        String json = readFixture("series.json").replace("82.5486", "82.12345678901234567890123456789");
        when(httpClient.get(SERIES_URI, HEADERS)).thenReturn(json);

        List<ApiRecord> records = source.fetch();

        assertEquals("kursrublya", source.getName());
        assertEquals(1, records.size());
        ApiRecord record = records.getFirst();
        assertEquals("kursrublya", record.source());
        assertEquals("Курс USD/RUB", record.title());
        assertEquals("https://kurs-rublya.ru", record.url());
        assertEquals(FETCHED_AT, record.fetchedAt());
        assertEquals(2, record.data().get("points").size());
        ApiRecord restoredRecord = jsonMapper.readValue(jsonMapper.writeValueAsString(record), ApiRecord.class);
        KursRublyaData restoredData = jsonMapper.treeToValue(restoredRecord.data(), KursRublyaData.class);
        assertEquals(mapper.toData(json), restoredData);
        assertEquals(new BigDecimal("82.12345678901234567890123456789"),
                restoredData.points().getFirst().ratePerUnit());
        assertEquals(LocalDate.of(2026, 11, 11), restoredData.points().getFirst().date());
        verify(httpClient).get(SERIES_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldUseConfiguredCurrencyAndDateParameters() throws IOException {
        URI uri = URI.create("https://kurs-rublya.ru/api/v1/rates/EUR/series/?from=2026-11-11&to=2026-11-12&limit=2");
        source = new KursRublyaSource(httpClient, mapper, jsonMapper, uri, clock);
        when(httpClient.get(uri, HEADERS)).thenReturn(readFixture("series.json").replace("USD", "EUR"));

        assertEquals("Курс EUR/RUB", source.fetch().getFirst().title());

        verify(httpClient).get(uri, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldReturnNoRecordsForEmptyHistory() throws IOException {
        when(httpClient.get(SERIES_URI, HEADERS)).thenReturn(readFixture("series-empty.json"));

        assertTrue(source.fetch().isEmpty());

        verify(httpClient).get(SERIES_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 429, 502})
    void shouldPropagateHttpFailureWithoutRetry(int status) {
        ApiRequestException failure = new ApiRequestException("Unexpected HTTP status: " + status, null);
        when(httpClient.get(SERIES_URI, HEADERS)).thenThrow(failure);

        assertSame(failure, assertThrows(ApiRequestException.class, source::fetch));

        verify(httpClient).get(SERIES_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldPropagateInvalidJsonWithoutRetry() {
        when(httpClient.get(SERIES_URI, HEADERS)).thenReturn("not json");

        assertThrows(JacksonException.class, source::fetch);

        verify(httpClient).get(SERIES_URI, HEADERS);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    void shouldPreserveInterruptionWithoutSendingRequest() {
        Thread.currentThread().interrupt();
        try {
            ApiRequestException failure = assertThrows(ApiRequestException.class, source::fetch);

            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(httpClient);
        } finally {
            Thread.interrupted();
        }
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/kursrublya/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
