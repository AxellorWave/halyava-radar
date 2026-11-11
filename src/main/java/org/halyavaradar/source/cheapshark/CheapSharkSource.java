package org.halyavaradar.source.cheapshark;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.ApiSource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CheapSharkSource implements ApiSource {
    private static final String SOURCE_NAME = "cheapshark";
    private static final Map<String, String> REQUEST_HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");

    private final ApiHttpClient httpClient;
    private final CheapSharkMapper mapper;
    private final JsonMapper jsonMapper;
    private final URI discountsUri;
    private final URI freeDealsUri;
    private final Clock clock;

    public CheapSharkSource(
            ApiHttpClient httpClient,
            CheapSharkMapper mapper,
            JsonMapper jsonMapper,
            URI discountsUri,
            URI freeDealsUri,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
        this.discountsUri = discountsUri;
        this.freeDealsUri = freeDealsUri;
        this.clock = clock;
    }

    @Override
    public String getName() {
        return SOURCE_NAME;
    }

    @Override
    public List<ApiRecord> fetch() {
        log.info("Fetching data from CheapShark");
        Map<String, String> storeNames = fetchStoreNames();
        Set<String> dealIds = fetchDealIds();
        List<ApiRecord> records = new ArrayList<>();
        for (String dealId : dealIds) {
            CheapSharkData data = fetchDeal(dealId, storeNames);
            if (data.salePrice().signum() == 0 || data.salePrice().compareTo(data.retailPrice()) < 0) {
                records.add(toRecord(data));
            }
        }
        log.info("CheapShark fetch completed: uniqueDeals={}, fetched={}, skipped={}",
                dealIds.size(), records.size(), dealIds.size() - records.size());
        return records;
    }

    private Map<String, String> fetchStoreNames() {
        URI uri = URI.create("https://www.cheapshark.com/api/1.0/stores");
        String json = httpClient.get(uri, REQUEST_HEADERS);
        return mapper.readStoreNames(json);
    }

    private Set<String> fetchDealIds() {
        Set<String> dealIds = new LinkedHashSet<>();
        String discountsJson = httpClient.get(discountsUri, REQUEST_HEADERS);
        dealIds.addAll(mapper.readDealIds(discountsJson));
        String freeDealsJson = httpClient.get(freeDealsUri, REQUEST_HEADERS);
        dealIds.addAll(mapper.readDealIds(freeDealsJson));
        return dealIds;
    }

    private CheapSharkData fetchDeal(String dealId, Map<String, String> storeNames) {
        URI uri = URI.create("https://www.cheapshark.com/api/1.0/deals?id=" + dealId);
        String json = httpClient.get(uri, REQUEST_HEADERS);
        return mapper.toData(dealId, json, storeNames);
    }

    private ApiRecord toRecord(CheapSharkData data) {
        return new ApiRecord(
                SOURCE_NAME,
                data.title(),
                "https://www.cheapshark.com/redirect?dealID=" + data.dealId(),
                clock.instant(),
                jsonMapper.valueToTree(data)
        );
    }
}
