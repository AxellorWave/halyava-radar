package org.halyavaradar.service;

import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.storage.FileStorage;
import org.halyavaradar.storage.WriteMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StorageServiceTest {
    private final FileStorage storage = mock(FileStorage.class);
    private final StorageService service = new StorageService(storage);
    private final List<ApiRecord> records = List.of(mock(ApiRecord.class));

    @Test
    void shouldSaveOnlyWhenOverwriting() throws IOException {
        service.write(records, WriteMode.OVERWRITE);

        verify(storage).save(records);
        verifyNoMoreInteractions(storage);
    }

    @Test
    void shouldAppendOnlyWhenAppending() throws IOException {
        service.write(records, WriteMode.APPEND);

        verify(storage).append(records);
        verifyNoMoreInteractions(storage);
    }

    @Test
    void shouldReturnLoadedRecords() throws IOException {
        when(storage.load()).thenReturn(records);

        assertSame(records, service.read());
        verify(storage).load();
        verifyNoMoreInteractions(storage);
    }

    @Test
    void shouldPropagateSaveFailure() throws IOException {
        IOException failure = new IOException("Ошибка записи");
        doThrow(failure).when(storage).save(records);

        assertSame(failure, assertThrows(IOException.class,
                () -> service.write(records, WriteMode.OVERWRITE)));
    }

    @Test
    void shouldPropagateAppendFailure() throws IOException {
        IOException failure = new IOException("Ошибка дозаписи");
        doThrow(failure).when(storage).append(records);

        assertSame(failure, assertThrows(IOException.class,
                () -> service.write(records, WriteMode.APPEND)));
    }

    @Test
    void shouldPropagateReadFailure() throws IOException {
        IOException failure = new IOException("Ошибка чтения");
        when(storage.load()).thenThrow(failure);

        assertSame(failure, assertThrows(IOException.class, service::read));
    }
}
