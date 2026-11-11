package org.halyavaradar.source.freetogame;

import org.halyavaradar.source.game.OfferType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FreeToGameMapper {
    private final JsonMapper jsonMapper;

    public FreeToGameMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<Integer> readGameIds(String json) {
        JsonNode node = jsonMapper.readTree(json);
        List<Integer> gameIds = new ArrayList<>();
        for (JsonNode gameNode : node) {
            gameIds.add(gameNode.get("id").asInt());
        }
        return gameIds;
    }

    public FreeToGameData toData(String json) {
        JsonNode node = jsonMapper.readTree(json);
        String releaseDateText = node.path("release_date").asString(null);
        LocalDate releaseDate = releaseDateText == null || releaseDateText.isBlank()
                ? null : LocalDate.parse(releaseDateText);
        FreeToGameRequirements requirements = null;
        JsonNode requirementsNode = node.get("minimum_system_requirements");
        if (requirementsNode != null && !requirementsNode.isNull()) {
            requirements = new FreeToGameRequirements(
                    requirementsNode.path("os").asString(null),
                    requirementsNode.path("processor").asString(null),
                    requirementsNode.path("memory").asString(null),
                    requirementsNode.path("graphics").asString(null),
                    requirementsNode.path("storage").asString(null)
            );
        }
        List<String> screenshotUrls = new ArrayList<>();
        JsonNode screenshotsNode = node.get("screenshots");
        if (screenshotsNode != null && !screenshotsNode.isNull()) {
            for (JsonNode screenshotNode : screenshotsNode) {
                String imageUrl = screenshotNode.path("image").asString(null);
                if (imageUrl != null && !imageUrl.isBlank()) {
                    screenshotUrls.add(imageUrl);
                }
            }
        }
        return new FreeToGameData(
                node.get("id").asInt(),
                node.get("title").asString(),
                node.path("thumbnail").asString(null),
                node.path("status").asString(null),
                node.path("short_description").asString(null),
                node.path("description").asString(null),
                node.path("game_url").asString(null),
                node.path("genre").asString(null),
                node.path("platform").asString(null),
                node.path("publisher").asString(null),
                node.path("developer").asString(null),
                releaseDate,
                node.path("freetogame_profile_url").asString(null),
                OfferType.FREE_TO_PLAY,
                requirements,
                List.copyOf(screenshotUrls)
        );
    }
}
