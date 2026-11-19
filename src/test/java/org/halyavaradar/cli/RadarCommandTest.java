package org.halyavaradar.cli;

import org.halyavaradar.config.ApplicationFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarCommandTest {
    @ParameterizedTest
    @ValueSource(strings = {"", "--help", "-h"})
    void shouldShowRootHelpAndRegisteredSubcommands(String argument) {
        try (ApplicationFactory factory = new ApplicationFactory()) {
            var cli = factory.createCommandLine();
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
}
