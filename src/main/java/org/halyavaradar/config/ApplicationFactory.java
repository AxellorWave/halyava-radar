package org.halyavaradar.config;

import org.halyavaradar.core.http.ApiHttpClient;
import org.halyavaradar.core.service.ApiAggregator;
import org.halyavaradar.core.service.StorageService;
import org.halyavaradar.core.service.StorageServiceFactory;
import org.halyavaradar.core.source.SourceRegistry;
import org.halyavaradar.core.source.cheapshark.CheapSharkMapper;
import org.halyavaradar.core.source.cheapshark.CheapSharkSource;
import org.halyavaradar.core.source.freetogame.FreeToGameMapper;
import org.halyavaradar.core.source.freetogame.FreeToGameSource;
import org.halyavaradar.core.source.kursrublya.KursRublyaMapper;
import org.halyavaradar.core.source.kursrublya.KursRublyaSource;
import org.halyavaradar.core.storage.CsvFileStorage;
import org.halyavaradar.core.storage.FileStorage;
import org.halyavaradar.core.storage.JsonFileStorage;
import org.halyavaradar.core.storage.StorageFormat;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class ApplicationFactory implements StorageServiceFactory, AutoCloseable {
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

    @Override
    public StorageService createStorageService(Path filePath, StorageFormat format) {
        FileStorage storage = switch (format) {
            case JSON -> new JsonFileStorage(filePath, jsonMapper);
            case CSV -> new CsvFileStorage(filePath, jsonMapper);
        };

        return new StorageService(storage);
    }

    public ApiAggregator createAggregator(SourceRegistry sourceRegistry) {
        return new ApiAggregator(sourceRegistry);
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }
}

