package org.halyavaradar.cli;

import org.halyavaradar.config.ApplicationFactory;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.service.StorageService;
import org.halyavaradar.storage.StorageFormat;
import picocli.CommandLine;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "read",
        description = "Прочитать сохранённые записи из файла.",
        synopsisHeading = "Использование: ",
        optionListHeading = "%nПараметры:%n"
)
public class ReadCommand implements Callable<Integer> {
    private final ApplicationFactory factory;
    private final JsonMapper jsonMapper;

    public ReadCommand(ApplicationFactory factory, JsonMapper jsonMapper) {
        this.factory = factory;
        this.jsonMapper = jsonMapper;
    }

    @CommandLine.Option(
            names = "--format",
            defaultValue = "JSON",
            description = "Формат файла: ${COMPLETION-CANDIDATES}. По умолчанию: ${DEFAULT-VALUE}."
    )
    private StorageFormat format;

    @CommandLine.Option(
            names = "--path",
            required = true,
            description = "Путь к файлу."
    )
    private Path filePath;

    @CommandLine.Option(
            names = "--source",
            description = "Показать записи только указанного источника."
    )
    private String source;

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
        StorageService storageService = factory.createStorageService(filePath, format);
        List<ApiRecord> records = storageService.read();
        if (source != null) {
            records = records.stream().filter(record -> source.equals(record.source())).toList();
        }

        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records);
        spec.commandLine().getOut().println(json);
        return 0;
    }
}
