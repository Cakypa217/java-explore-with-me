package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.model.dto.client.EndpointHit;
import ru.practicum.model.dto.client.ViewStats;
import ru.practicum.model.dto.client.ViewStatsRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class StatsClient {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate;
    private final String statsServiceUri;

    public StatsClient(RestTemplate restTemplate,
                       @Value("${services.stats-service.uri:http://stats-server:9090}") String statsServiceUri) {
        this.restTemplate = restTemplate;
        this.statsServiceUri = statsServiceUri;
    }

    public void hit(EndpointHit hit) {
        try {
            restTemplate.postForEntity(statsServiceUri + "/hit", hit, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке запроса в stats-service", e);
        }
    }

    public List<ViewStats> getStats(ViewStatsRequest request) {
        StringBuilder url = new StringBuilder(statsServiceUri + "/stats?start=" +
                request.getStart().format(DTF) + "&end=" +
                request.getEnd().format(DTF) +
                "&unique=" + request.isUnique());

        if (request.getUris() != null && !request.getUris().isEmpty()) {
            request.getUris().forEach(uri -> url.append("&uris=").append(uri));
        }

        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url.toString(), ViewStats[].class);
        return List.of(Optional.ofNullable(response.getBody()).orElse(new ViewStats[0]));
    }
}