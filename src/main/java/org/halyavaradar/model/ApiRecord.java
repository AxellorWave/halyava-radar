package org.halyavaradar.model;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record ApiRecord(
        String source,
        String title,
        String url,
        Instant fetchedAt,
        JsonNode data
) {
}
