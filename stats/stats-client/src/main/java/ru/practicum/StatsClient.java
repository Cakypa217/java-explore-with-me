package ru.practicum;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StatsClient {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String application;
    private final String statsServiceUri;
    private final ObjectMapper json;
    private final HttpClient httpClient;

    public StatsClient(@Value("${spring.application.name}") String aplication,
                       @Value("${services.stats-service.uri:http://localhost:9090}") String statsServiceUri,
                       ObjectMapper json) {
        this.application = aplication;
        this.statsServiceUri = statsServiceUri;
        this.json = json;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public void hit(HttpServletRequest userRequest) {
        EndpointHit hit = EndpointHit.builder()
                .app(application)
                .uri(userRequest.getRequestURI())
                .ip(userRequest.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        try {
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest
                    .BodyPublishers
                    .ofString(json.writeValueAsString(hit));
            HttpRequest hitRequest = HttpRequest.newBuilder()
                    .uri(URI.create(statsServiceUri + "/hit"))
                    .POST(bodyPublisher)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .build();

            HttpResponse<Void> response = httpClient.send(hitRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке запроса на сохранение статистики", e);
        }
    }

    public List<ViewStats> getStats(ViewStatsRequest request) {
        try {
            String queryString = toQueryString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(statsServiceUri + "/stats" + queryString))
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return json.readValue(response.body(), new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении статистики", e);
        }
        return Collections.emptyList();
    }

    private String toQueryString(ViewStatsRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("start", request.getStart().format(DTF));
        params.put("end", request.getEnd().format(DTF));
        params.put("unique", String.valueOf(request.isUnique()));

        String paramString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        String uris = (request.getUris() != null && !request.getUris().isEmpty())
                ? request.getUris().stream().map(uri -> "uris=" + uri).collect(Collectors.joining("&"))
                : "";

        return "?" + paramString + (uris.isEmpty() ? "" : "&" + uris);
    }
}
