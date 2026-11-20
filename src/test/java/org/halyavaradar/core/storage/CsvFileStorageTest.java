package org.halyavaradar.core.storage;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.core.model.ApiRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFileStorageTest {
    private static final String HEADER = "source,title,url,fetchedAt,data\n";

    @TempDir
    Path tempDir;

    private final JsonMapper jsonMapper = JsonMapperFactory.create();

    @Test
    void shouldSaveAndLoadRecordsPreservingOrderAndNestedData() throws Exception {
        Path file = tempDir.resolve("records.csv");
        List<ApiRecord> records = List.of(record("Первая игра"), record("Вторая игра"));

        new CsvFileStorage(file, jsonMapper).save(records);

        assertEquals(records, new CsvFileStorage(file, jsonMapper).load());
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("Первая игра"));
        try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file, StandardCharsets.UTF_8))
                .withCSVParser(new RFC4180ParserBuilder().build()).build()) {
            assertArrayEquals(new String[]{"source", "title", "url", "fetchedAt", "data"}, reader.readNext());
            String[] row = reader.readNext();
            assertEquals(5, row.length);
            assertEquals("example", row[0]);
            assertEquals("Первая игра", row[1]);
            assertEquals("https://example.com/game", row[2]);
            assertEquals("2026-11-19T12:00:00.123456789Z", row[3]);
            assertEquals(records.getFirst().data(), jsonMapper.readTree(row[4]));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Игра, дополнение", "Игра \"Deluxe\"", "Первая строка\nВторая строка", "", "C:\\Games\\Game"})
    void shouldPreserveSpecialCharactersAndEmptyTitle(String title) throws Exception {
        CsvFileStorage storage = storage();
        List<ApiRecord> records = List.of(record(title));

        storage.save(records);

        assertEquals(records, storage.load());
    }

    @Test
    void shouldReadCsvWrittenOutsideStorage() throws Exception {
        Path file = tempDir.resolve("records.csv");
        Files.writeString(file, HEADER
                + "example,Игра,https://example.com/game,2026-11-19T12:00:00Z,{}\n", StandardCharsets.UTF_8);

        assertEquals(List.of(new ApiRecord("example", "Игра", "https://example.com/game",
                Instant.parse("2026-11-19T12:00:00Z"), jsonMapper.readTree("{}"))),
                new CsvFileStorage(file, jsonMapper).load());
    }

    @Test
    void shouldCreateMissingParentDirectories() throws Exception {
        Path file = tempDir.resolve("data/storage/records.csv");
        CsvFileStorage storage = new CsvFileStorage(file, jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));

        storage.save(records);

        assertTrue(Files.isRegularFile(file));
        assertEquals(records, storage.load());
    }

    @Test
    void shouldReplacePreviousRecords() throws Exception {
        CsvFileStorage storage = storage();
        storage.save(List.of(record("Старая игра"), record("Ещё одна старая игра")));
        List<ApiRecord> replacement = List.of(record("Новая игра"));

        storage.save(replacement);

        assertEquals(replacement, storage.load());
    }

    @Test
    void shouldReplaceRecordsWithHeaderOnlyWhenSavingEmptyList() throws Exception {
        CsvFileStorage storage = storage();
        storage.save(List.of(record("Игра")));

        storage.save(List.of());

        assertTrue(storage.load().isEmpty());
        assertEquals(1, readRows(tempDir.resolve("records.csv")).size());
    }

    @Test
    void shouldReturnEmptyListWithoutCreatingMissingFile() throws Exception {
        Path file = tempDir.resolve("missing/records.csv");

        assertTrue(new CsvFileStorage(file, jsonMapper).load().isEmpty());
        assertFalse(Files.exists(file.getParent()));
    }

    @Test
    void shouldAppendWithoutRepeatingHeaderOrLosingEarlierRecords() throws Exception {
        CsvFileStorage storage = storage();
        ApiRecord first = record("Первая игра");
        ApiRecord second = record("Игра, \"Deluxe\"\nВторая строка");
        ApiRecord third = record("Третья игра");
        storage.save(List.of(first));

        storage.append(List.of(second));
        storage.append(List.of(third));

        assertEquals(List.of(first, second, third), storage.load());
        List<String[]> rows = readRows(tempDir.resolve("records.csv"));
        assertEquals(4, rows.size());
        assertEquals(1L, rows.stream().filter(row -> row[0].equals("source")).count());
    }

    @Test
    void shouldCreateFileAndDirectoriesWhenAppendingForFirstTime() throws Exception {
        Path file = tempDir.resolve("missing/storage/records.csv");
        CsvFileStorage storage = new CsvFileStorage(file, jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));

        storage.append(records);

        assertEquals(records, storage.load());
        assertEquals(2, readRows(file).size());
    }

    @Test
    void shouldWriteHeaderAndRecordsWhenAppendingToEmptyFile() throws Exception {
        Path file = Files.createFile(tempDir.resolve("records.csv"));
        CsvFileStorage storage = new CsvFileStorage(file, jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));

        storage.append(records);

        assertEquals(records, storage.load());
        assertEquals(2, readRows(file).size());
    }

    @Test
    void shouldLeaveExistingFileUnchangedWhenAppendingEmptyList() throws Exception {
        Path file = tempDir.resolve("records.csv");
        CsvFileStorage storage = new CsvFileStorage(file, jsonMapper);
        storage.save(List.of(record("Игра")));
        String original = Files.readString(file, StandardCharsets.UTF_8);

        storage.append(List.of());

        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "title,source,url,fetchedAt,data\n"})
    void shouldRejectMissingOrIncorrectHeaderOnLoad(String content) throws IOException {
        Path file = tempDir.resolve("records.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertThrows(IOException.class, new CsvFileStorage(file, jsonMapper)::load);
    }

    @Test
    void shouldRejectIncorrectHeaderOnAppendWithoutChangingFile() throws IOException {
        Path file = tempDir.resolve("records.csv");
        String original = "title,source,url,fetchedAt,data\n";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        CsvFileStorage storage = new CsvFileStorage(file, jsonMapper);

        assertThrows(IOException.class, () -> storage.append(List.of(record("Игра"))));
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "example,title,url,2026-11-19T12:00:00Z\n",
            "example,title,url,2026-11-19T12:00:00Z,{},extra\n"
    })
    void shouldRejectIncorrectColumnCount(String row) throws IOException {
        CsvFileStorage storage = storageWithRow(row);

        assertThrows(IllegalArgumentException.class, storage::load);
    }

    @Test
    void shouldRejectInvalidTimestamp() throws IOException {
        CsvFileStorage storage = storageWithRow("example,title,url,not-a-date,{}\n");

        assertThrows(DateTimeParseException.class, storage::load);
    }

    @Test
    void shouldRejectInvalidJsonData() throws IOException {
        CsvFileStorage storage = storageWithRow("example,title,url,2026-11-19T12:00:00Z,{broken\n");

        assertThrows(JacksonException.class, storage::load);
    }

    @Test
    void shouldRejectUnclosedQuotedField() throws IOException {
        CsvFileStorage storage = storageWithRow("example,\"unclosed title");

        assertThrows(IOException.class, storage::load);
    }

    @Test
    void shouldPropagateReadFailureWhenPathIsDirectory() {
        CsvFileStorage storage = new CsvFileStorage(tempDir, jsonMapper);

        assertThrows(IOException.class, storage::load);
    }

    @Test
    void shouldPropagateWriteFailureWhenParentIsFile() throws IOException {
        Path parent = Files.createFile(tempDir.resolve("parent"));
        CsvFileStorage storage = new CsvFileStorage(parent.resolve("records.csv"), jsonMapper);

        assertThrows(IOException.class, () -> storage.save(List.of(record("Игра"))));
        assertThrows(IOException.class, () -> storage.append(List.of(record("Игра"))));
    }

    private CsvFileStorage storage() {
        return new CsvFileStorage(tempDir.resolve("records.csv"), jsonMapper);
    }

    private CsvFileStorage storageWithRow(String row) throws IOException {
        Files.writeString(tempDir.resolve("records.csv"), HEADER + row, StandardCharsets.UTF_8);
        return storage();
    }

    private List<String[]> readRows(Path file) throws IOException, CsvException {
        try (CSVReader reader = new CSVReaderBuilder(Files.newBufferedReader(file, StandardCharsets.UTF_8))
                .withCSVParser(new RFC4180ParserBuilder().build()).build()) {
            return reader.readAll();
        }
    }

    private ApiRecord record(String title) {
        return new ApiRecord("example", title, "https://example.com/game",
                Instant.parse("2026-11-19T12:00:00.123456789Z"),
                jsonMapper.readTree("""
                        {
                          "price": 0.1234567890123456789,
                          "details": {
                            "platforms": ["PC", "Linux"],
                            "description": "Игра, \\"Deluxe\\"\\nВторая строка",
                            "optional": null
                          }
                        }
                        """));
    }
}
