package org.halyavaradar.config;

import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;

public final class JsonMapperFactory {
    private JsonMapperFactory() {
    }

    public static JsonMapper create() {
        return JsonMapper.builder()
                .enable(JsonNodeFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();
    }
}
