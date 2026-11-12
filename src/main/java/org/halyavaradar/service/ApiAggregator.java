package org.halyavaradar.service;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.ApiSource;
import org.halyavaradar.source.SourceRegistry;

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
        log.info("Fetching data from sources: count={}", sourceNames.size());
        for (String sourceName : sourceNames) {
            ApiSource source = sourceRegistry.getSource(sourceName);
            try {
                records.addAll(source.fetch());
            } catch (RuntimeException e) {
                log.warn("Source fetch failed: source={}, errorType={}", sourceName, e.getClass().getSimpleName());
            }
        }
        log.info("Aggregation completed: records={}", records.size());

        return records;
    }

}
