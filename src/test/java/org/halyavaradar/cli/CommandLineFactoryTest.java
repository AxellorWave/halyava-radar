package org.halyavaradar.cli;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.service.StorageServiceFactory;
import org.halyavaradar.core.source.SourceRegistry;
import org.halyavaradar.core.storage.StorageFormat;
import org.halyavaradar.core.storage.WriteMode;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CommandLineFactoryTest {
    private final CommandLine cli = new CommandLineFactory(
            mock(ApiAggregator.class), mock(SourceRegistry.class),
            mock(StorageServiceFactory.class), JsonMapperFactory.create()).create();

    @Test
    void shouldRegisterFetchAndReadSubcommands() {
        assertEquals(Set.of("fetch", "read"), cli.getSubcommands().keySet());
        assertInstanceOf(FetchCommand.class, cli.getSubcommands().get("fetch").getCommand());
        assertInstanceOf(ReadCommand.class, cli.getSubcommands().get("read").getCommand());
    }

    @Test
    void shouldAllowCaseInsensitiveEnumsInSubcommands() {
        assertTrue(cli.isCaseInsensitiveEnumValuesAllowed());
        CommandLine.ParseResult fetch = cli.parseArgs(
                "fetch", "--path", "records.csv", "--format", "cSv", "--mode", "aPpEnD").subcommand();
        assertEquals(StorageFormat.CSV, fetch.matchedOptionValue("--format", null));
        assertEquals(WriteMode.APPEND, fetch.matchedOptionValue("--mode", null));

        CommandLine.ParseResult read = cli.parseArgs(
                "read", "--path", "records.json", "--format", "jSoN").subcommand();
        assertEquals(StorageFormat.JSON, read.matchedOptionValue("--format", null));
    }
}
