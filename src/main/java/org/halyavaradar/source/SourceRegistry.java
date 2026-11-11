package org.halyavaradar.source;

import java.util.*;

public class SourceRegistry {
    private final Map<String, ApiSource> sources;

    public SourceRegistry(List<ApiSource> sources) {
        this.sources = new LinkedHashMap<>();
        for (ApiSource source : sources) {
            if (this.sources.containsKey(source.getName())) {
                throw new IllegalArgumentException("Source " + source.getName() + " already exists");
            }
            this.sources.put(source.getName(), source);
        }
    }

    public ApiSource getSource(String name) {
        ApiSource result = this.sources.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Source " + name + " does not exist");
        }
        return result;
    }

    public Set<String> getSourceNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(sources.keySet()));
    }


}
