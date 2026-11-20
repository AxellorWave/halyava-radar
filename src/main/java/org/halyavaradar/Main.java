package org.halyavaradar;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.cli.CommandLineFactory;
import org.halyavaradar.config.ApplicationFactory;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.source.SourceRegistry;

@Slf4j
public class Main {
    public static void main(String[] args) {
        int exitCode;
        try (ApplicationFactory factory = new ApplicationFactory()) {
            SourceRegistry registry = factory.createSourceRegistry();
            ApiAggregator aggregator = factory.createAggregator(registry);
            exitCode = new CommandLineFactory(aggregator, registry, factory,
                    factory.getJsonMapper()).create().execute(args);
        } catch (RuntimeException e) {
            log.error("Не удалось запустить или завершить приложение", e);
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
