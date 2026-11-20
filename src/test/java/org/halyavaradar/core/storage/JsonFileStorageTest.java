package org.halyavaradar.core.storage;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.core.model.ApiRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileStorageTest {
    @TempDir
    Path tempDir;

    private final JsonMapper jsonMapper = JsonMapperFactory.create();

    @Test
    void shouldSaveAndLoadRecordsPreservingOrderAndData() throws IOException {
        Path file = tempDir.resolve("records.json");
        List<ApiRecord> records = List.of(record("Первая игра"), record("Вторая игра"));

        new JsonFileStorage(file, jsonMapper).save(records);

        assertEquals(records, new JsonFileStorage(file, jsonMapper).load());
        String json = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(json.contains("Первая игра"));
        assertTrue(jsonMapper.readTree(json).isArray());
    }

    @Test
    void shouldCreateMissingParentDirectories() throws IOException {
        Path file = tempDir.resolve("data/storage/records.json");
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));

        storage.save(records);

        assertTrue(Files.isRegularFile(file));
        assertEquals(records, storage.load());
    }

    @Test
    void shouldReplacePreviousRecordsInsteadOfAppending() throws IOException {
        JsonFileStorage storage = new JsonFileStorage(tempDir.resolve("records.json"), jsonMapper);
        storage.save(List.of(record("Старая игра"), record("Ещё одна старая игра")));
        List<ApiRecord> replacement = List.of(record("Новая игра"));

        storage.save(replacement);

        assertEquals(replacement, storage.load());
    }

    @Test
    void shouldReplaceExistingRecordsWithEmptyArray() throws IOException {
        Path file = tempDir.resolve("records.json");
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);
        storage.save(List.of(record("Игра")));

        storage.save(List.of());

        assertEquals(jsonMapper.readTree("[]"), jsonMapper.readTree(Files.readString(file)));
        assertTrue(storage.load().isEmpty());
    }

    @Test
    void shouldReturnEmptyListWithoutCreatingMissingFile() throws IOException {
        Path file = tempDir.resolve("missing/records.json");

        assertTrue(new JsonFileStorage(file, jsonMapper).load().isEmpty());
        assertFalse(Files.exists(file.getParent()));
    }

    @Test
    void shouldRejectMalformedJson() throws IOException {
        Path file = tempDir.resolve("records.json");
        Files.writeString(file, "[{broken json", StandardCharsets.UTF_8);
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);

        assertThrows(JacksonException.class, storage::load);
    }

    @Test
    void shouldRejectEmptyFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("records.json"));
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);

        assertThrows(JacksonException.class, storage::load);
    }

    @Test
    void shouldPropagateReadFailureWhenPathIsDirectory() {
        JsonFileStorage storage = new JsonFileStorage(tempDir, jsonMapper);

        assertThrows(IOException.class, storage::load);
    }

    @Test
    void shouldPropagateWriteFailureWhenParentIsFile() throws IOException {
        Path parent = Files.createFile(tempDir.resolve("parent"));
        JsonFileStorage storage = new JsonFileStorage(parent.resolve("records.json"), jsonMapper);

        assertThrows(IOException.class, () -> storage.save(List.of(record("Игра"))));
    }

    @Test
    void shouldAppendRecordsPreservingEarlierRecordsAndOrder() throws IOException {
        JsonFileStorage storage = new JsonFileStorage(tempDir.resolve("records.json"), jsonMapper);
        ApiRecord first = record("Первая игра");
        ApiRecord second = record("Вторая игра");
        ApiRecord third = record("Третья игра");
        storage.save(List.of(first));

        storage.append(List.of(second));
        storage.append(List.of(third));

        assertEquals(List.of(first, second, third), storage.load());
    }

    @Test
    void shouldCreateFileAndDirectoriesOnFirstAppend() throws IOException {
        Path file = tempDir.resolve("missing/storage/records.json");
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));

        storage.append(records);

        assertTrue(Files.isRegularFile(file));
        assertEquals(records, storage.load());
    }

    @Test
    void shouldKeepExistingRecordsWhenAppendingEmptyList() throws IOException {
        JsonFileStorage storage = new JsonFileStorage(tempDir.resolve("records.json"), jsonMapper);
        List<ApiRecord> records = List.of(record("Игра"));
        storage.save(records);

        storage.append(List.of());

        assertEquals(records, storage.load());
    }

    @Test
    void shouldNotOverwriteMalformedJsonOnAppend() throws IOException {
        Path file = tempDir.resolve("records.json");
        String original = "[{broken json";
        Files.writeString(file, original, StandardCharsets.UTF_8);
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);

        assertThrows(JacksonException.class, () -> storage.append(List.of(record("Игра"))));
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void shouldNotOverwriteEmptyFileOnAppend() throws IOException {
        Path file = Files.createFile(tempDir.resolve("records.json"));
        JsonFileStorage storage = new JsonFileStorage(file, jsonMapper);

        assertThrows(JacksonException.class, () -> storage.append(List.of(record("Игра"))));
        assertEquals(0L, Files.size(file));
    }

    private ApiRecord record(String title) {
        return new ApiRecord(
                "example", title, "https://example.com/game",
                Instant.parse("2026-11-19T12:00:00.123456789Z"),
                jsonMapper.readTree("""
                        {
                          "price": 0.1234567890123456789,
                          "details": {"platforms": ["PC"], "description": null}
                        }
                        """)
        );
    }
}
