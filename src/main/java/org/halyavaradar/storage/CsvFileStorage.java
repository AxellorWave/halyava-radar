package org.halyavaradar.storage;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.halyavaradar.model.ApiRecord;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvFileStorage implements FileStorage {
    private static final String[] HEADER = {"source", "title", "url", "fetchedAt", "data"};
    private final Path filePath;
    private final JsonMapper jsonMapper;

    public CsvFileStorage(Path filePath, JsonMapper jsonMapper) {
        this.filePath = filePath;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void save(List<ApiRecord> records) throws IOException {
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(filePath, StandardCharsets.UTF_8))) {
            writer.writeNext(HEADER);
            for (ApiRecord record : records) {
                writer.writeNext(toRow(record));
            }
            if (writer.checkError()) {
                throw new IOException("Не удалось записать CSV-файл: " + filePath, writer.getException());
            }
        }
    }

    @Override
    public void append(List<ApiRecord> records) throws IOException {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            save(records);
            return;
        }
        if (!checkFirstRow()) {
            throw new IOException("Некорректный заголовок в CSV: " + filePath);
        }

        try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.APPEND))) {
            for (ApiRecord record : records) {
                writer.writeNext(toRow(record));
            }
            if (writer.checkError()) {
                throw new IOException("Не удалось записать CSV-файл: " + filePath, writer.getException());
            }
        }
    }

    private boolean checkFirstRow() throws IOException {
        try (CSVReader reader = createReader()) {
            return Arrays.equals(reader.readNext(), HEADER);
        } catch (CsvValidationException e) {
            throw new IOException("Не удалось прочитать заголовок CSV: " + filePath, e);
        }
    }

    @Override
    public List<ApiRecord> load() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        List<ApiRecord> records = new ArrayList<>();
        try (CSVReader reader = createReader()) {
            String[] header = reader.readNext();
            if (!Arrays.equals(header, HEADER)) {
                throw new IOException("Некорректный заголовок в CSV: " + filePath);
            }
            String[] row;
            while ((row = reader.readNext()) != null) {
                records.add(fromRaw(row));
            }
        } catch (CsvValidationException e) {
            throw new IOException("Не удалось прочитать CSV-файл: " + filePath, e);
        }
        return records;
    }

    private CSVReader createReader() throws IOException {
        return new CSVReaderBuilder(Files.newBufferedReader(filePath, StandardCharsets.UTF_8))
                .withCSVParser(new RFC4180ParserBuilder().build())
                .build();
    }

    private String[] toRow(ApiRecord record) {
        return new String[]{
                record.source(),
                record.title(),
                record.url(),
                record.fetchedAt().toString(),
                jsonMapper.writeValueAsString(record.data())
        };
    }

    private ApiRecord fromRaw(String[] row) {
        if (row.length != 5) {
            throw new IllegalArgumentException("Ожидалось 5 колонок, получено: " + row.length);
        }
        return new ApiRecord(row[0], row[1], row[2], Instant.parse(row[3]), jsonMapper.readTree(row[4]));
    }
}
