package org.halyavaradar.cli;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.service.StorageServiceFactory;
import org.halyavaradar.core.source.SourceRegistry;
import picocli.CommandLine;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class CommandLineFactory {
    private final ApiAggregator aggregator;
    private final SourceRegistry sourceRegistry;
    private final StorageServiceFactory storageServiceFactory;
    private final JsonMapper jsonMapper;

    public CommandLineFactory(ApiAggregator aggregator, SourceRegistry sourceRegistry,
                              StorageServiceFactory storageServiceFactory, JsonMapper jsonMapper) {
        this.aggregator = aggregator;
        this.sourceRegistry = sourceRegistry;
        this.storageServiceFactory = storageServiceFactory;
        this.jsonMapper = jsonMapper;
    }

    public CommandLine create() {
        CommandLine commandLine = new CommandLine(new RadarCommand());

        commandLine.addSubcommand(
                "fetch",
                new FetchCommand(aggregator, sourceRegistry, storageServiceFactory)
        );
        commandLine.addSubcommand(
                "read",
                new ReadCommand(storageServiceFactory, jsonMapper)
        );

        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setParameterExceptionHandler((exception, arguments) -> {
            CommandLine failedCommand = exception.getCommandLine();
            log.warn("Ошибка аргументов команды {}: {}",
                    failedCommand.getCommandName(), exception.getMessage());
            failedCommand.getErr().println("Ошибка аргументов: " + exception.getMessage());
            failedCommand.usage(failedCommand.getErr());
            return CommandLine.ExitCode.USAGE;
        });
        commandLine.setExecutionExceptionHandler((exception, failedCommand, parseResult) -> {
            log.error("Ошибка выполнения команды {}: {}",
                    failedCommand.getCommandName(), exception.getMessage(), exception);
            failedCommand.getErr().println("Ошибка выполнения: " + exception.getMessage());
            return CommandLine.ExitCode.SOFTWARE;
        });

        return commandLine;
    }
}
