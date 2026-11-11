package org.halyavaradar.source;

import org.halyavaradar.model.ApiRecord;

import java.util.List;

public interface ApiSource {
    String getName();

    List<ApiRecord> fetch();
}
