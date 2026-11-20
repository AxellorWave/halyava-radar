package org.halyavaradar.core.service;

import org.halyavaradar.core.storage.StorageFormat;

import java.nio.file.Path;

public interface StorageServiceFactory {
    StorageService createStorageService(Path filePath, StorageFormat format);
}
