package org.halyavaradar.source.kursrublya;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.http.ApiRequestException;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.ApiSource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;

@Slf4j
public class KursRublyaSource implements ApiSource {
    private static final String SOURCE_NAME = "kursrublya";
    private static final Map<String, String> REQUEST_HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");

    private final ApiHttpClient httpClient;
    private final KursRublyaMapper mapper;
    private final JsonMapper jsonMapper;
    private final URI seriesUri;
    private final Clock clock;

    public KursRublyaSource(
            ApiHttpClient httpClient,
            KursRublyaMapper mapper,
            JsonMapper jsonMapper,
            URI seriesUri,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
        this.seriesUri = seriesUri;
        this.clock = clock;
    }

    @Override
    public String getName() {
        return SOURCE_NAME;
    }

    @Override
    public List<ApiRecord> fetch() {
        log.info("Fetching exchange rate history from KursRublya");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiRequestException("KursRublya request interrupted", e);
        }
        KursRublyaData data = mapper.toData(httpClient.get(seriesUri, REQUEST_HEADERS));
        if (data.points().isEmpty()) {
            log.info("KursRublya fetch completed: currency={}, points=0, fetched=0", data.currencyCode());
            return List.of();
        }
        ApiRecord record = new ApiRecord(
                SOURCE_NAME,
                "Курс " + data.currencyCode() + "/" + data.quoteCurrency(),
                data.siteUrl(),
                clock.instant(),
                jsonMapper.valueToTree(data)
        );
        log.info("KursRublya fetch completed: currency={}, points={}, fetched=1",
                data.currencyCode(), data.points().size());
        return List.of(record);
    }
}
