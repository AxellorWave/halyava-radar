package org.halyavaradar.core.storage;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.core.model.ApiRecord;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JsonFileStorage implements FileStorage {
    private final Path filePath;
    private final JsonMapper jsonMapper;

    public JsonFileStorage(Path filePath, JsonMapper jsonMapper) {
        this.filePath = filePath;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void save(List<ApiRecord> records) throws IOException {
        log.debug("Сохранение JSON: файл={}, записей={}", filePath, records.size());
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records);
        Files.writeString(filePath, json, StandardCharsets.UTF_8);
    }

    @Override
    public List<ApiRecord> load() throws IOException {
        log.debug("Чтение JSON: файл={}", filePath);
        if (!Files.exists(filePath)) {
            log.warn("JSON-файл отсутствует, возвращён пустой список: {}", filePath);
            return new ArrayList<>();
        }
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        return jsonMapper.readValue(json, new TypeReference<List<ApiRecord>>() {
        });
    }

    @Override
    public void append(List<ApiRecord> records) throws IOException {
        log.debug("Дозапись JSON: файл={}, новых записей={}", filePath, records.size());
        List<ApiRecord> oldRecords = new ArrayList<>(load());
        oldRecords.addAll(records);
        save(oldRecords);
    }
}


