package org.halyavaradar.config;

import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.service.ApiAggregator;
import org.halyavaradar.service.StorageService;
import org.halyavaradar.source.SourceRegistry;
import org.halyavaradar.source.cheapshark.CheapSharkMapper;
import org.halyavaradar.source.cheapshark.CheapSharkSource;
import org.halyavaradar.source.freetogame.FreeToGameMapper;
import org.halyavaradar.source.freetogame.FreeToGameSource;
import org.halyavaradar.source.kursrublya.KursRublyaMapper;
import org.halyavaradar.source.kursrublya.KursRublyaSource;
import org.halyavaradar.storage.CsvFileStorage;
import org.halyavaradar.storage.FileStorage;
import org.halyavaradar.storage.JsonFileStorage;
import org.halyavaradar.storage.StorageFormat;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class ApplicationFactory implements AutoCloseable {
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final ApiHttpClient apiHttpClient;
    private final Clock clock;

    public ApplicationFactory() {
        jsonMapper = JsonMapperFactory.create();
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        apiHttpClient = new ApiHttpClient(httpClient, Duration.ofSeconds(20));
        clock = Clock.systemUTC();
    }

    @Override
    public void close() {
        httpClient.close();
    }

    public SourceRegistry createSourceRegistry() {
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

    public ApiAggregator createApiAggregator() {
        return new ApiAggregator(createSourceRegistry());
    }

    public StorageService createStorageService(Path filePath, StorageFormat format) {
        FileStorage storage = switch (format) {
            case JSON -> new JsonFileStorage(filePath, jsonMapper);
            case CSV -> new CsvFileStorage(filePath, jsonMapper);
        };

        return new StorageService(storage);
    }
}

