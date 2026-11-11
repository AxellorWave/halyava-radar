package org.halyavaradar.source.freetogame;

import lombok.extern.slf4j.Slf4j;
import org.halyavaradar.http.ApiHttpClient;
import org.halyavaradar.http.ApiRequestException;
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
public class FreeToGameSource implements ApiSource {
    private static final String SOURCE_NAME = "freetogame";
    private static final Map<String, String> REQUEST_HEADERS = Map.of("User-Agent", "HalyavaRadar/1.0");

    private final ApiHttpClient httpClient;
    private final FreeToGameMapper mapper;
    private final JsonMapper jsonMapper;
    private final URI gamesUri;
    private final int maxGames;
    private final Clock clock;

    public FreeToGameSource(
            ApiHttpClient httpClient,
            FreeToGameMapper mapper,
            JsonMapper jsonMapper,
            URI gamesUri,
            int maxGames,
            Clock clock
    ) {
        if (maxGames <= 0) {
            throw new IllegalArgumentException("Game limit must be positive");
        }
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
        this.gamesUri = gamesUri;
        this.maxGames = maxGames;
        this.clock = clock;
    }

    @Override
    public String getName() {
        return SOURCE_NAME;
    }

    @Override
    public List<ApiRecord> fetch() {
        log.info("Fetching data from FreeToGame: limit={}", maxGames);
        String json = httpClient.get(gamesUri, REQUEST_HEADERS);
        Set<Integer> gameIds = new LinkedHashSet<>(mapper.readGameIds(json));
        List<ApiRecord> records = new ArrayList<>();
        for (int gameId : gameIds) {
            if (records.size() >= maxGames) {
                break;
            }
            FreeToGameData data = fetchGame(gameId);
            records.add(toRecord(data));
        }
        log.info("FreeToGame fetch completed: available={}, selected={}, fetched={}",
                gameIds.size(), Math.min(gameIds.size(), maxGames), records.size());
        return records;
    }

    private FreeToGameData fetchGame(int gameId) {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiRequestException("FreeToGame request interrupted", e);
        }
        URI uri = URI.create("https://www.freetogame.com/api/game?id=" + gameId);
        String json = httpClient.get(uri, REQUEST_HEADERS);
        return mapper.toData(json);
    }

    private ApiRecord toRecord(FreeToGameData data) {
        return new ApiRecord(
                SOURCE_NAME,
                data.title(),
                data.profileUrl(),
                clock.instant(),
                jsonMapper.valueToTree(data)
        );
    }
}
