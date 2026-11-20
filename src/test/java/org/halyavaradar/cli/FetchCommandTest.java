package org.halyavaradar.cli;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.core.model.ApiRecord;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.service.StorageService;
import org.halyavaradar.core.service.StorageServiceFactory;
import org.halyavaradar.core.source.SourceRegistry;
import org.halyavaradar.core.storage.StorageFormat;
import org.halyavaradar.core.storage.WriteMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FetchCommandTest {
    @TempDir
    Path tempDir;

    private final ApiAggregator aggregator = mock(ApiAggregator.class);
    private final SourceRegistry registry = mock(SourceRegistry.class);
    private final StorageServiceFactory storageServiceFactory = mock(StorageServiceFactory.class);
    private final StorageService storage = mock(StorageService.class);
    private final List<String> allSources = List.of("cheapshark", "freetogame", "kursrublya");
    private final List<ApiRecord> records = List.of(new ApiRecord(
            "cheapshark", "Игра", "https://example.com/game", Instant.parse("2026-11-19T12:00:00Z"),
            JsonMapperFactory.create().readTree("{}")));
    private final StringWriter output = new StringWriter();
    private CommandLine cli;
    private Path file;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("records.dat");
        when(registry.getSourceNames()).thenReturn(new LinkedHashSet<>(allSources));
        cli = new CommandLine(new FetchCommand(aggregator, registry, storageServiceFactory));
        cli.setCaseInsensitiveEnumValuesAllowed(true);
        cli.setOut(new PrintWriter(output, true));
        cli.setErr(new PrintWriter(new StringWriter(), true));
    }

    @Test
    void shouldFetchAllSourcesAndOverwriteJsonByDefault() throws IOException {
        when(aggregator.fetch(allSources)).thenReturn(records);
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);

        assertEquals(0, cli.execute("--path", file.toString()));

        verify(aggregator).fetch(allSources);
        verify(storage).write(records, WriteMode.OVERWRITE);
    }

    @ParameterizedTest
    @CsvSource({"json, overwrite", "JSON, APPEND", "csv, overwrite", "CsV, aPpEnD"})
    void shouldPassSelectedSourcesFormatAndModeToServices(String format, String mode) throws IOException {
        List<String> selected = List.of("freetogame", "cheapshark");
        StorageFormat expectedFormat = StorageFormat.valueOf(format.toUpperCase(java.util.Locale.ROOT));
        WriteMode expectedMode = WriteMode.valueOf(mode.toUpperCase(java.util.Locale.ROOT));
        when(aggregator.fetch(selected)).thenReturn(records);
        when(storageServiceFactory.createStorageService(file, expectedFormat)).thenReturn(storage);

        assertEquals(0, cli.execute("--sources", "freetogame,cheapshark",
                "--format", format, "--mode", mode, "--path", file.toString()));

        verify(aggregator).fetch(selected);
        verify(storageServiceFactory).createStorageService(file, expectedFormat);
        verify(storage).write(records, expectedMode);
    }

    @Test
    void shouldValidateAllSourcesBeforeCallingServices() {
        assertEquals(CommandLine.ExitCode.USAGE,
                cli.execute("--sources", "cheapshark,unknown", "--path", file.toString()));

        verifyNoInteractions(aggregator, storageServiceFactory, storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--format", "--mode"})
    void shouldRejectUnknownEnumValuesBeforeCallingServices(String option) {
        assertEquals(CommandLine.ExitCode.USAGE,
                cli.execute("--path", file.toString(), option, "invalid"));

        verifyNoInteractions(aggregator, storageServiceFactory, storage);
    }

    @Test
    void shouldRequirePathBeforeCallingServices() {
        assertEquals(CommandLine.ExitCode.USAGE, cli.execute());
        verifyNoInteractions(aggregator, storageServiceFactory, storage);
    }

    @Test
    void shouldShowHelpWithoutPathAndWithoutCallingServices() {
        assertEquals(0, cli.execute("--help"));
        assertTrue(output.toString().contains("--sources"));
        assertTrue(output.toString().contains("--mode"));
        verifyNoInteractions(aggregator, storageServiceFactory, storage);
    }

    @Test
    void shouldSaveEmptyResult() throws IOException {
        when(aggregator.fetch(allSources)).thenReturn(List.of());
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);

        assertEquals(0, cli.execute("--path", file.toString()));
        verify(storage).write(List.of(), WriteMode.OVERWRITE);
    }

    @Test
    void shouldReturnFailureWhenSavingFails() throws IOException {
        when(aggregator.fetch(allSources)).thenReturn(records);
        when(storageServiceFactory.createStorageService(file, StorageFormat.JSON)).thenReturn(storage);
        doThrow(new IOException("Ошибка записи")).when(storage).write(records, WriteMode.OVERWRITE);

        assertEquals(CommandLine.ExitCode.SOFTWARE, cli.execute("--path", file.toString()));
        verify(storage).write(records, WriteMode.OVERWRITE);
    }
}
