package org.halyavaradar.cli;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.config.ApplicationFactory;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.service.ApiAggregator;
import org.halyavaradar.service.StorageService;
import org.halyavaradar.source.SourceRegistry;
import org.halyavaradar.storage.StorageFormat;
import org.halyavaradar.storage.WriteMode;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "fetch",
        description = "Получить данные из выбранных источников.",
        synopsisHeading = "Использование: ",
        optionListHeading = "%nПараметры:%n"
)
public class FetchCommand implements Callable<Integer> {
    private final ApiAggregator aggregator;
    private final SourceRegistry sourceRegistry;
    private final ApplicationFactory factory;

    public FetchCommand(ApiAggregator aggregator, SourceRegistry sourceRegistry, ApplicationFactory factory) {
        this.aggregator = aggregator;
        this.sourceRegistry = sourceRegistry;
        this.factory = factory;
    }

    @CommandLine.Option(
            names = "--sources",
            split = ",",
            description = "Источники через запятую. По умолчанию — все."
    )
    private List<String> sources;

    @CommandLine.Option(
            names = "--format",
            defaultValue = "JSON",
            description = "Формат файла: ${COMPLETION-CANDIDATES}. По умолчанию: ${DEFAULT-VALUE}."
    )
    private StorageFormat format;

    @CommandLine.Option(
            names = "--mode",
            defaultValue = "OVERWRITE",
            description = "Режим записи: ${COMPLETION-CANDIDATES}. По умолчанию: ${DEFAULT-VALUE}."
    )
    private WriteMode mode;

    @CommandLine.Option(
            names = "--path",
            required = true,
            description = "Путь к файлу."
    )
    private Path filePath;

    @CommandLine.Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Показать справку."
    )
    private boolean help;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws IOException {
        Set<String> availableSources = sourceRegistry.getSourceNames();
        List<String> selectedSources = sources == null ? List.copyOf(availableSources) : sources;
        for (String source : selectedSources) {
            if (!availableSources.contains(source)) {
                throw new CommandLine.ParameterException(
                        spec.commandLine(), "Неизвестный источник: " + source +
                        ". Доступные: " + String.join(", ", availableSources)
                );
            }
        }

        List<ApiRecord> records = aggregator.fetch(selectedSources);
        StorageService storageService = factory.createStorageService(filePath, format);
        storageService.write(records, mode);

        log.info("Сохранено записей: {}, формат: {}, режим: {}, файл: {}",
                records.size(), format, mode, filePath);

        return 0;
    }

}
