package org.halyavaradar;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.ApiSource;
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

            for (ApiSource source : List.of(cheapSharkSource, freeToGameSource, kursRublyaSource)) {
                System.out.println("Получение данных: " + source.getName());
                List<ApiRecord> records = source.fetch();
                System.out.println("Получено записей: " + records.size());
                System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(records));
            }
        }
    }
}
