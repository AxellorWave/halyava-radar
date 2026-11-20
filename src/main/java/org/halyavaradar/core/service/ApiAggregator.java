package org.halyavaradar.core.service;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.core.model.ApiRecord;
import org.halyavaradar.core.source.ApiSource;
import org.halyavaradar.core.source.SourceRegistry;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApiAggregator {
    private final SourceRegistry sourceRegistry;

    public ApiAggregator(SourceRegistry sourceRegistry) {
        this.sourceRegistry = sourceRegistry;
    }

    public List<ApiRecord> fetch(List<String> sourceNames) {
        List<ApiRecord> records = new ArrayList<>();
        int failedSources = 0;
        log.info("Начат сбор данных: источников={}", sourceNames.size());
        for (String sourceName : sourceNames) {
            ApiSource source = sourceRegistry.getSource(sourceName);
            try {
                records.addAll(source.fetch());
            } catch (RuntimeException e) {
                failedSources++;
                log.warn("Не удалось получить данные источника {}. Сбор остальных источников продолжается",
                        sourceName, e);
            }
        }
        if (!sourceNames.isEmpty() && failedSources == sourceNames.size()) {
            log.error("Сбор данных завершён: все {} источников завершились ошибкой, записей нет", failedSources);
        } else {
            log.info("Сбор данных завершён: записей={}, источников с ошибкой={}", records.size(), failedSources);
        }

        return records;
    }

}
