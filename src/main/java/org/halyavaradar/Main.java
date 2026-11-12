package org.halyavaradar;

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
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        JsonMapper jsonMapper = JsonMapperFactory.create();
        try (HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            ApiHttpClient apiHttpClient = new ApiHttpClient(httpClient, Duration.ofSeconds(20));
            Clock clock = Clock.systemUTC();
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

            SourceRegistry sourceRegistry = new SourceRegistry(
                    List.of(cheapSharkSource, freeToGameSource, kursRublyaSource)
            );
            ApiAggregator aggregator = new ApiAggregator(sourceRegistry);
            List<String> sourceNames = List.copyOf(sourceRegistry.getSourceNames());

            System.out.println("Получение данных из источников: " + String.join(", ", sourceNames));
            List<ApiRecord> records = aggregator.fetch(sourceNames);
            System.out.println("Получено записей: " + records.size());
            System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records));
        }
    }
}
