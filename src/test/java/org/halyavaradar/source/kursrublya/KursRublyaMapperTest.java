package org.halyavaradar.source.kursrublya;

import org.halyavaradar.config.JsonMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KursRublyaMapperTest {
    private final JsonMapper jsonMapper = JsonMapperFactory.create();
    private final KursRublyaMapper mapper = new KursRublyaMapper(jsonMapper);

    @Test
    void shouldReadHistoryAndSourceMetadata() throws IOException {
        KursRublyaData data = mapper.toData(readFixture("series.json"));

        assertEquals(new KursRublyaData(
                "1.1.0", "USD", "RUB",
                LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12), 2,
                List.of(
                        new KursRublyaPoint(LocalDate.of(2026, 11, 11), new BigDecimal("82.5486")),
                        new KursRublyaPoint(LocalDate.of(2026, 11, 12), new BigDecimal("82.6617"))
                ),
                "https://www.cbr.ru/hd_base/valuta/", "Данные ЦБ РФ",
                "https://creativecommons.org/licenses/by/4.0/", "https://kurs-rublya.ru"
        ), data);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "CNY", "JPY"})
    void shouldReadCurrencyFromResponse(String currency) throws IOException {
        KursRublyaData data = mapper.toData(readFixture("series.json").replace("USD", currency));

        assertEquals(currency, data.currencyCode());
        assertEquals("RUB", data.quoteCurrency());
    }

    @Test
    void shouldPreservePrecisionThroughJsonRoundTrip() throws IOException {
        String json = readFixture("series.json").replace("82.5486", "82.12345678901234567890123456789");
        KursRublyaData data = mapper.toData(json);
        KursRublyaData restored = jsonMapper.readValue(jsonMapper.writeValueAsString(data), KursRublyaData.class);

        assertEquals(new BigDecimal("82.12345678901234567890123456789"), data.points().getFirst().ratePerUnit());
        assertEquals(data, restored);
    }

    @Test
    void shouldReadEmptyHistoryWithUnknownDates() throws IOException {
        KursRublyaData data = mapper.toData(readFixture("series-empty.json"));

        assertEquals(0, data.count());
        assertTrue(data.points().isEmpty());
        assertNull(data.from());
        assertNull(data.to());
    }

    @Test
    void shouldKeepMissingMetadataAsNull() {
        KursRublyaData data = mapper.toData("""
                {"code":"EUR","count":0,"points":[],"apiVersion":null}
                """);

        assertNull(data.apiVersion());
        assertNull(data.sourceUrl());
        assertNull(data.attribution());
        assertNull(data.licenseUrl());
        assertNull(data.siteUrl());
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThrows(JacksonException.class, () -> mapper.toData("not json"));
    }

    @Test
    void shouldRejectInvalidPointDate() throws IOException {
        String json = readFixture("series.json").replace("2026-11-11", "2026-99-99");

        assertThrows(DateTimeParseException.class, () -> mapper.toData(json));
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/kursrublya/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
