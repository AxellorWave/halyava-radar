package org.halyavaradar.service;

import org.halyavaradar.config.JsonMapperFactory;
import org.halyavaradar.http.ApiRequestException;
import org.halyavaradar.model.ApiRecord;
import org.halyavaradar.source.ApiSource;
import org.halyavaradar.source.SourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiAggregatorTest {
    private ApiSource first;
    private ApiSource second;
    private ApiSource third;
    private ApiAggregator aggregator;

    @BeforeEach
    void setUp() {
        first = createSource("first");
        second = createSource("second");
        third = createSource("third");
        aggregator = new ApiAggregator(new SourceRegistry(List.of(first, second, third)));
    }

    @Test
    void shouldCombineRecordsInRequestedSourceOrder() {
        ApiRecord firstRecord = createRecord("first", "First record");
        ApiRecord secondRecord = createRecord("second", "Second record");
        ApiRecord thirdRecord = createRecord("second", "Third record");
        when(first.fetch()).thenReturn(List.of(firstRecord));
        when(second.fetch()).thenReturn(List.of(secondRecord, thirdRecord));

        assertEquals(List.of(secondRecord, thirdRecord, firstRecord), aggregator.fetch(List.of("second", "first")));

        InOrder order = inOrder(first, second);
        order.verify(second).fetch();
        order.verify(first).fetch();
    }

    @Test
    void shouldKeepEarlierRecordsAndContinueAfterFailure() {
        ApiRecord firstRecord = createRecord("first", "First record");
        ApiRecord thirdRecord = createRecord("third", "Third record");
        when(first.fetch()).thenReturn(List.of(firstRecord));
        when(second.fetch()).thenThrow(new ApiRequestException("HTTP request failed", null));
        when(third.fetch()).thenReturn(List.of(thirdRecord));

        assertEquals(List.of(firstRecord, thirdRecord), aggregator.fetch(List.of("first", "second", "third")));

        InOrder order = inOrder(first, second, third);
        order.verify(first).fetch();
        order.verify(second).fetch();
        order.verify(third).fetch();
    }

    @Test
    void shouldReturnEmptyListWhenNoSourcesSelected() {
        assertTrue(aggregator.fetch(List.of()).isEmpty());

        verify(first, never()).fetch();
        verify(second, never()).fetch();
        verify(third, never()).fetch();
    }

    @Test
    void shouldContinueAfterEmptySourceResult() {
        ApiRecord record = createRecord("second", "Example record");
        when(first.fetch()).thenReturn(List.of());
        when(second.fetch()).thenReturn(List.of(record));

        assertEquals(List.of(record), aggregator.fetch(List.of("first", "second")));

        verify(first).fetch();
        verify(second).fetch();
    }

    @Test
    void shouldRejectUnknownSourceInsteadOfSwallowingRegistryError() {
        assertThrows(IllegalArgumentException.class, () -> aggregator.fetch(List.of("unknown", "first")));

        verify(first, never()).fetch();
        verify(second, never()).fetch();
        verify(third, never()).fetch();
    }

    @Test
    void shouldFetchOnlySelectedSources() {
        ApiRecord record = createRecord("second", "Example record");
        when(second.fetch()).thenReturn(List.of(record));

        assertEquals(List.of(record), aggregator.fetch(List.of("second")));

        verify(second).fetch();
        verify(first, never()).fetch();
        verify(third, never()).fetch();
    }

    @Test
    void shouldReturnEmptyListWhenAllSelectedSourcesFail() {
        when(first.fetch()).thenThrow(new ApiRequestException("HTTP request failed", null));
        when(second.fetch()).thenThrow(new IllegalArgumentException("Invalid response data"));

        assertTrue(aggregator.fetch(List.of("first", "second")).isEmpty());

        verify(first).fetch();
        verify(second).fetch();
    }

    private ApiSource createSource(String name) {
        ApiSource source = mock(ApiSource.class);
        when(source.getName()).thenReturn(name);
        return source;
    }

    private ApiRecord createRecord(String source, String title) {
        return new ApiRecord(
                source,
                title,
                "https://example.com/" + source,
                Instant.parse("2026-11-12T12:00:00Z"),
                JsonMapperFactory.create().readTree("{\"value\":42}")
        );
    }
}
