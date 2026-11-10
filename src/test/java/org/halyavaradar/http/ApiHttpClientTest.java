package org.halyavaradar.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiHttpClientTest {
    private HttpClient httpClient;
    private HttpResponse<String> response;
    private ApiHttpClient client;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        httpClient = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        client = new ApiHttpClient(httpClient, Duration.ofSeconds(15));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 204, 299})
    void shouldReturnBodyForSuccessfulStatus(int status) throws Exception {
        String body = status == 204 ? "" : "{\"offers\":[]}";
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);

        String result = client.get(URI.create("https://example.com/offers"), Map.of());

        assertEquals(body, result);
    }

    @Test
    void shouldSendGetRequestWithUriHeadersAndTimeout() throws Exception {
        URI uri = URI.create("https://example.com/offers?category=shooter&title=Half%20Life");
        Map<String, String> headers = Map.of(
                "Accept", "application/json",
                "User-Agent", "HalyavaRadar"
        );
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("[]");
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);

        client.get(uri, headers);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest request = captor.getValue();
        assertEquals(uri, request.uri());
        assertEquals("GET", request.method());
        assertEquals(Duration.ofSeconds(15), request.timeout().orElseThrow());
        assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
        assertEquals("HalyavaRadar", request.headers().firstValue("User-Agent").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(ints = {199, 300, 404, 429, 500})
    void shouldRejectUnsuccessfulStatus(int status) throws Exception {
        when(response.statusCode()).thenReturn(status);
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> client.get(URI.create("https://example.com/offers"), Map.of()));

        assertEquals("Unexpected HTTP status: " + status, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldPreserveIOExceptionAsCause() throws Exception {
        IOException cause = new IOException("Connection failed");
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(cause);

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> client.get(URI.create("https://example.com/offers"), Map.of()));

        assertEquals("HTTP request failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldPreserveTimeoutAsCause() throws Exception {
        HttpTimeoutException cause = new HttpTimeoutException("Request timed out");
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(cause);

        ApiRequestException exception = assertThrows(ApiRequestException.class,
                () -> client.get(URI.create("https://example.com/offers"), Map.of()));

        assertEquals("HTTP request failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldRestoreInterruptFlagAndPreserveCause() throws Exception {
        InterruptedException cause = new InterruptedException("Request interrupted");
        when(httpClient.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(cause);

        try {
            ApiRequestException exception = assertThrows(ApiRequestException.class,
                    () -> client.get(URI.create("https://example.com/offers"), Map.of()));

            assertEquals("HTTP request interrupted", exception.getMessage());
            assertSame(cause, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
