package org.halyavaradar.service;

import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.storage.FileStorage;
import org.halyavaradar.storage.WriteMode;

import java.io.IOException;
import java.util.List;

public class StorageService {
    private final FileStorage storage;

    public StorageService(FileStorage storage) {
        this.storage = storage;
    }

    public void write(List<ApiRecord> records, WriteMode mode) throws IOException {
        switch (mode) {
            case OVERWRITE -> storage.save(records);
            case APPEND -> storage.append(records);
        }
    }

    public List<ApiRecord> read() throws IOException {
        return storage.load();
    }
}
