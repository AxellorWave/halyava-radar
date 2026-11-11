package org.halyavaradar.source.kursrublya;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KursRublyaMapper {
    private final JsonMapper jsonMapper;

    public KursRublyaMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public KursRublyaData toData(String json) {
        JsonNode node = jsonMapper.readTree(json);
        List<KursRublyaPoint> points = new ArrayList<>();
        for (JsonNode point : node.get("points")) {
            points.add(new KursRublyaPoint(
                    LocalDate.parse(point.get("dateIso").asString()),
                    point.get("ratePerUnit").asDecimal()
            ));
        }
        return new KursRublyaData(
                node.path("apiVersion").asString(null),
                node.get("code").asString(),
                "RUB",
                readDate(node, "from"),
                readDate(node, "to"),
                node.get("count").asInt(),
                List.copyOf(points),
                node.path("sourceUrl").asString(null),
                node.path("attribution").asString(null),
                node.path("license").asString(null),
                node.path("siteUrl").asString(null)
        );
    }

    private LocalDate readDate(JsonNode node, String field) {
        String value = node.path(field).asString(null);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
