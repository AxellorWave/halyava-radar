package org.halyavaradar.service;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.storage.FileStorage;
import org.halyavaradar.storage.WriteMode;

import java.io.IOException;
import java.util.List;

@Slf4j
public class StorageService {
    private final FileStorage storage;

    public StorageService(FileStorage storage) {
        this.storage = storage;
    }

    public void write(List<ApiRecord> records, WriteMode mode) throws IOException {
        log.info("Запись файла: хранилище={}, режим={}, записей={}",
                storage.getClass().getSimpleName(), mode, records.size());
        switch (mode) {
            case OVERWRITE -> storage.save(records);
            case APPEND -> storage.append(records);
        }
        log.info("Запись файла завершена: записей={}", records.size());
    }

    public List<ApiRecord> read() throws IOException {
        log.info("Чтение файла: хранилище={}", storage.getClass().getSimpleName());
        List<ApiRecord> records = storage.load();
        log.info("Чтение файла завершено: записей={}", records.size());
        return records;
    }
}
