package org.halyavaradar.cli;

import org.halyavaradar.config.ApplicationFactory;
import org.halyavaradar.config.JsonMapperFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliErrorHandlingTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldHandleReadFailureWithoutStackTraceInCommandErrorStream() {
        try (ApplicationFactory factory = new ApplicationFactory()) {
            CommandLine cli = factory.createCommandLine();
            StringWriter errors = new StringWriter();
            cli.setErr(new PrintWriter(errors, true));

            assertEquals(CommandLine.ExitCode.SOFTWARE,
                    cli.execute("read", "--format", "csv", "--path", tempDir.toString()));
            assertTrue(errors.toString().contains("Ошибка выполнения:"));
            assertFalse(errors.toString().contains("\tat "));
        }
    }

    @Test
    void shouldHandleUnknownSourceBeforeSaving() {
        Path file = tempDir.resolve("records.json");
        try (ApplicationFactory factory = new ApplicationFactory()) {
            CommandLine cli = factory.createCommandLine();
            StringWriter errors = new StringWriter();
            cli.setErr(new PrintWriter(errors, true));

            assertEquals(CommandLine.ExitCode.USAGE,
                    cli.execute("fetch", "--sources", "unknown", "--path", file.toString()));
            assertTrue(errors.toString().contains("Неизвестный источник: unknown"));
            assertTrue(errors.toString().contains("Использование:"));
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void shouldHandleMissingRequiredArgument() {
        try (ApplicationFactory factory = new ApplicationFactory()) {
            CommandLine cli = factory.createCommandLine();
            StringWriter errors = new StringWriter();
            cli.setErr(new PrintWriter(errors, true));

            assertEquals(CommandLine.ExitCode.USAGE, cli.execute("read"));
            assertTrue(errors.toString().contains("Ошибка аргументов:"));
        }
    }

    @Test
    void shouldKeepJsonOutputSeparateFromServiceLogs() throws Exception {
        Path file = tempDir.resolve("records.json");
        Files.writeString(file, "[]");
        try (ApplicationFactory factory = new ApplicationFactory()) {
            CommandLine cli = factory.createCommandLine();
            StringWriter output = new StringWriter();
            StringWriter errors = new StringWriter();
            cli.setOut(new PrintWriter(output, true));
            cli.setErr(new PrintWriter(errors, true));

            assertEquals(CommandLine.ExitCode.OK,
                    cli.execute("read", "--format", "json", "--path", file.toString()));
            assertEquals(JsonMapperFactory.create().readTree("[]"),
                    JsonMapperFactory.create().readTree(output.toString()));
            assertTrue(errors.toString().isEmpty());
        }
    }
}
