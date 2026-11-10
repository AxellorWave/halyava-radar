package org.halyavaradar.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class ApiHttpClient {
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public ApiHttpClient(HttpClient httpClient, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    public String get(URI uri, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(requestTimeout).GET();
        headers.forEach((name, value) -> builder.header(name, value));
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new ApiRequestException("Unexpected HTTP status: " + response.statusCode(), null);
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiRequestException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiRequestException("HTTP request interrupted", e);
        }

    }
}

