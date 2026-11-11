package org.halyavaradar.source.freetogame;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.source.game.OfferType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeToGameMapperTest {
    private final FreeToGameMapper mapper = new FreeToGameMapper(JsonMapperFactory.create());

    @Test
    void shouldReadGameIdsInResponseOrder() throws IOException {
        List<Integer> ids = mapper.readGameIds(readFixture("games.json"));

        assertEquals(List.of(655, 655, 656, 657), ids);
    }

    @Test
    void shouldReturnEmptyListForEmptyGamesArray() {
        assertTrue(mapper.readGameIds("[]").isEmpty());
    }

    @Test
    void shouldMapAllGameFieldsRequirementsAndScreenshots() throws IOException {
        FreeToGameData result = mapper.toData(readFixture("detail-full.json"));

        FreeToGameData expected = new FreeToGameData(
                655,
                "Example Shooter",
                "https://example.com/655/thumbnail.jpg",
                "Live",
                "A free multiplayer shooter.",
                "Описание игры.\nA second paragraph.",
                "https://www.freetogame.com/open/example-shooter",
                "Shooter",
                "Windows",
                "Example Publisher",
                "Example Developer",
                LocalDate.of(2026, 10, 30),
                "https://www.freetogame.com/example-shooter",
                OfferType.FREE_TO_PLAY,
                new FreeToGameRequirements("Windows 10", "Four-core CPU", "8 GB RAM",
                        "4 GB graphics card", "25 GB available space"),
                List.of("https://example.com/655/screenshot-1.jpg", "https://example.com/655/screenshot-2.jpg")
        );
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"detail-minimal.json", "detail-null.json"})
    void shouldPreserveUnknownOptionalValues(String fixture) throws IOException {
        FreeToGameData result = mapper.toData(readFixture(fixture));

        FreeToGameData expected = new FreeToGameData(
                656,
                "Example Browser Game",
                null,
                null,
                null,
                null,
                "https://www.freetogame.com/open/example-browser-game",
                null,
                null,
                null,
                null,
                null,
                "https://www.freetogame.com/example-browser-game",
                OfferType.FREE_TO_PLAY,
                null,
                List.of()
        );
        assertEquals(expected, result);
    }

    @Test
    void shouldPreservePartialRequirementsWithoutInventingMissingValues() throws IOException {
        FreeToGameData result = mapper.toData(readFixture("detail-partial.json"));

        assertEquals(new FreeToGameRequirements(null, null, "8 GB RAM", null, null),
                result.minimumSystemRequirements());
        assertNull(result.releaseDate());
        assertEquals(List.of("https://example.com/658/screenshot.jpg"), result.screenshotUrls());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "not json"})
    void shouldRejectMalformedJson(String json) {
        assertThrows(JacksonException.class, () -> mapper.readGameIds(json));
        assertThrows(JacksonException.class, () -> mapper.toData(json));
    }

    @Test
    void shouldRejectInvalidReleaseDate() throws IOException {
        String json = readFixture("detail-full.json").replace("2026-10-30", "invalid-date");

        assertThrows(DateTimeParseException.class, () -> mapper.toData(json));
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/freetogame/" + name)) {
            assertNotNull(input, "Missing test fixture: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
