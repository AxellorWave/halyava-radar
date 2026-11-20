package org.halyavaradar.core.source;

import org.halyavaradar.core.model.ApiRecord;

import java.util.List;

public interface ApiSource {
    String getName();

    List<ApiRecord> fetch();
}
