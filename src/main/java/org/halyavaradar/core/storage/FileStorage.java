package org.halyavaradar.core.storage;

import org.halyavaradar.core.model.ApiRecord;

import java.io.IOException;
import java.util.List;

public interface FileStorage {
    void save(List<ApiRecord> records) throws IOException;

    void append(List<ApiRecord> records) throws IOException;

    List<ApiRecord> load() throws IOException;
}
