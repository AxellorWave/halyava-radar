package org.halyavaradar;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.service.ApiAggregator;
import org.halyavaradar.source.SourceRegistry;
import org.halyavaradar.source.cheapshark.CheapSharkMapper;
import org.halyavaradar.source.cheapshark.CheapSharkSource;
import org.halyavaradar.source.freetogame.FreeToGameMapper;
import org.halyavaradar.source.freetogame.FreeToGameSource;
import org.halyavaradar.source.kursrublya.KursRublyaMapper;
import org.halyavaradar.source.kursrublya.KursRublyaSource;
import org.halyavaradar.storage.CsvFileStorage;
import org.halyavaradar.storage.JsonFileStorage;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Slf4j
public class Main {
    private static final Path RECORDS_JSON_PATH = Path.of("data", "records.json");
    private static final Path RECORDS_CSV_PATH = Path.of("data", "records.csv");

    public static void main(String[] args) throws IOException {
        JsonMapper jsonMapper = JsonMapperFactory.create();
        try (HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            ApiHttpClient apiHttpClient = new ApiHttpClient(httpClient, Duration.ofSeconds(20));
            SourceRegistry sourceRegistry = createSourceRegistry(apiHttpClient, jsonMapper, Clock.systemUTC());
            List<ApiRecord> records = fetchRecords(sourceRegistry);
            saveRecords(records, jsonMapper);
            log.debug("Полученные записи:\n{}", jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records));
        }
    }

    private static SourceRegistry createSourceRegistry(ApiHttpClient apiHttpClient, JsonMapper jsonMapper, Clock clock) {
        CheapSharkSource cheapSharkSource = new CheapSharkSource(
                apiHttpClient,
                new CheapSharkMapper(jsonMapper),
                jsonMapper,
                URI.create("https://www.cheapshark.com/api/1.0/deals?storeID=7&upperPrice=10&pageSize=2&onSale=1"),
                URI.create("https://www.cheapshark.com/api/1.0/deals?upperPrice=0&pageSize=2"),
                clock
        );
        FreeToGameSource freeToGameSource = new FreeToGameSource(
                apiHttpClient,
                new FreeToGameMapper(jsonMapper),
                jsonMapper,
                URI.create("https://www.freetogame.com/api/games?platform=pc&category=shooter&sort-by=release-date"),
                2,
                clock
        );

        KursRublyaSource kursRublyaSource = new KursRublyaSource(
                apiHttpClient,
                new KursRublyaMapper(jsonMapper),
                jsonMapper,
                URI.create("https://kurs-rublya.ru/api/v1/rates/USD/series/?limit=3"),
                clock
        );

        return new SourceRegistry(List.of(cheapSharkSource, freeToGameSource, kursRublyaSource));
    }

    private static List<ApiRecord> fetchRecords(SourceRegistry sourceRegistry) {
        ApiAggregator aggregator = new ApiAggregator(sourceRegistry);
        List<String> sourceNames = List.copyOf(sourceRegistry.getSourceNames());

        log.info("Получение данных из источников: {}", String.join(", ", sourceNames));
        List<ApiRecord> records = aggregator.fetch(sourceNames);
        log.info("Получено записей: {}", records.size());
        return records;
    }

    private static void saveRecords(List<ApiRecord> records, JsonMapper jsonMapper) throws IOException {
        JsonFileStorage jsonStorage = new JsonFileStorage(RECORDS_JSON_PATH, jsonMapper);
        jsonStorage.save(records);
        log.info("Данные json сохранены в {}", RECORDS_JSON_PATH);
        CsvFileStorage csvStorage = new CsvFileStorage(RECORDS_CSV_PATH, jsonMapper);
        csvStorage.save(records);
        log.info("Данные csv сохранены в {}", RECORDS_CSV_PATH);
    }

}
