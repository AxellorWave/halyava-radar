package org.halyavaradar.cli;

import org.halyavaradar.config.ApplicationFactory;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.source.SourceRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarCommandTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "--help", "-h"})
    void shouldShowRootHelpAndRegisteredSubcommands(String argument) {
        try (ApplicationFactory factory = new ApplicationFactory()) {
            var cli = createCli(factory);
            StringWriter output = new StringWriter();
            StringWriter errors = new StringWriter();
            cli.setOut(new PrintWriter(output, true));
            cli.setErr(new PrintWriter(errors, true));

            int exitCode = argument.isEmpty() ? cli.execute() : cli.execute(argument);

            assertEquals(0, exitCode);
            assertTrue(output.toString().contains("Использование:"));
            assertTrue(output.toString().contains("fetch"));
            assertTrue(output.toString().contains("read"));
            assertTrue(errors.toString().isEmpty());
        }
    }

    private CommandLine createCli(ApplicationFactory factory) {
        SourceRegistry registry = factory.createSourceRegistry();
        ApiAggregator aggregator = factory.createAggregator(registry);
        return new CommandLineFactory(aggregator, registry, factory, factory.getJsonMapper()).create();
    }
}
