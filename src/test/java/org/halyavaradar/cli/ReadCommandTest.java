package org.halyavaradar.cli;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.core.model.ApiRecord;
import org.halyavaradar.core.service.StorageService;
import org.halyavaradar.core.service.StorageServiceFactory;
import org.halyavaradar.core.storage.StorageFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReadCommandTest {
    @TempDir
    Path tempDir;

    private final StorageServiceFactory storageServiceFactory = mock(StorageServiceFactory.class);
    private final StorageService storage = mock(StorageService.class);
    private final JsonMapper mapper = JsonMapperFactory.create();
    private final StringWriter output = new StringWriter();
    private final List<ApiRecord> records = List.of(record("cheapshark", "Первая"),
            record("freetogame", "Вторая"), record("cheapshark", "Третья"));
    private CommandLine cli;
    private Path file;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("records.dat");
        cli = new CommandLine(new ReadCommand(storageServiceFactory, mapper));
        cli.setCaseInsensitiveEnumValuesAllowed(true);
        cli.setOut(new PrintWriter(output, true));
        cli.setErr(new PrintWriter(new StringWriter(), true));
    }

    @Test
    void shouldReadJsonByDefaultAndPrintAllRecords() throws IOException {
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);
        when(storage.read()).thenReturn(records);

        assertEquals(0, cli.execute("--path", file.toString()));

        assertEquals(records, readOutput());
        verify(storageServiceFactory).createStorageService(file, StorageFormat.JSON);
        verify(storage).read();
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "CsV"})
    void shouldSelectFormatAndFilterRecordsPreservingOrder(String format) throws IOException {
        StorageFormat expected = StorageFormat.valueOf(format.toUpperCase(java.util.Locale.ROOT));
        when(storageServiceFactory.createStorageService(file, expected)).thenReturn(storage);
        when(storage.read()).thenReturn(records);

        assertEquals(0, cli.execute("--path", file.toString(), "--format", format, "--source", "cheapshark"));

        assertEquals(List.of(records.get(0), records.get(2)), readOutput());
        assertEquals(3, records.size());
        verify(storageServiceFactory).createStorageService(file, expected);
    }

    @Test
    void shouldReturnEmptyArrayWhenSourceDoesNotMatch() throws IOException {
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);
        when(storage.read()).thenReturn(records);

        assertEquals(0, cli.execute("--path", file.toString(), "--source", "archived-source"));
        assertEquals(List.of(), readOutput());
    }

    @Test
    void shouldAllowFilteringSourceAbsentFromCurrentRegistry() throws IOException {
        List<ApiRecord> archived = List.of(record("archived-source", "Архив"));
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);
        when(storage.read()).thenReturn(archived);

        assertEquals(0, cli.execute("--path", file.toString(), "--source", "archived-source"));
        assertEquals(archived, readOutput());
    }

    @Test
    void shouldShowHelpWithoutReadingFile() {
        assertEquals(0, cli.execute("--help"));
        assertTrue(output.toString().contains("--source"));
        verifyNoInteractions(storageServiceFactory, storage);
    }

    @Test
    void shouldRejectMissingPathWithoutReadingFile() {
        assertEquals(CommandLine.ExitCode.USAGE, cli.execute());
        verifyNoInteractions(storageServiceFactory, storage);
    }

    @Test
    void shouldRejectUnknownFormatWithoutReadingFile() {
        assertEquals(CommandLine.ExitCode.USAGE, cli.execute("--path", file.toString(), "--format", "xml"));
        verifyNoInteractions(storageServiceFactory, storage);
    }

    @Test
    void shouldReturnFailureWithoutOutputWhenReadFails() throws IOException {
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);
        when(storage.read()).thenThrow(new IOException("Ошибка чтения"));

        assertEquals(CommandLine.ExitCode.SOFTWARE, cli.execute("--path", file.toString()));
        assertTrue(output.toString().isEmpty());
    }

    private List<ApiRecord> readOutput() {
        return mapper.readValue(output.toString(), new TypeReference<List<ApiRecord>>() {});
    }

    private ApiRecord record(String source, String title) {
        return new ApiRecord(source, title, "https://example.com/game",
                Instant.parse("2026-11-19T12:00:00Z"), mapper.readTree("{\"price\":1.99}"));
    }
}
