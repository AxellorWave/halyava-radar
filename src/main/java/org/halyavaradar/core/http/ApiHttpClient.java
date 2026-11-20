package org.halyavaradar.core.http;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
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
            log.debug("Отправка HTTP GET: сервер={}", uri.getHost());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                log.debug("Сервер вернул неуспешный HTTP-статус: сервер={}, статус={}",
                        uri.getHost(), response.statusCode());
                throw new ApiRequestException("Unexpected HTTP status: " + response.statusCode(), null);
            }
            log.debug("HTTP GET завершён: сервер={}, статус={}", uri.getHost(), response.statusCode());
            return response.body();
        } catch (IOException e) {
            log.debug("Ошибка HTTP GET: сервер={}", uri.getHost(), e);
            throw new ApiRequestException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("HTTP GET прерван: сервер={}", uri.getHost(), e);
            throw new ApiRequestException("HTTP request interrupted", e);
        }

    }
}

