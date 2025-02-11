package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.repository.StatsRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;
    private final EndpointHitMapper mapper;

    @Override
    public void recordHit(EndpointHit hit) {
        repository.save(mapper.toEntity(hit));
    }

    @Override
    public List<ViewStats> calculateViews(ViewStatsRequest request) {
        List<Object[]> rawStats = request.getUris().isEmpty()
                ? repository.findViewStatsWithoutUris(
                request.getStart(),
                request.getEnd(),
                request.isUnique())
                : repository.findViewStatsWithUris(
                request.getStart(),
                request.getEnd(),
                request.getUris(),
                request.isUnique());

        return buildViewStatsList(rawStats);
    }

    private List<ViewStats> buildViewStatsList(List<Object[]> rawStats) {
        return Objects.requireNonNullElse(rawStats, Collections.emptyList()).stream()
                .map(obj -> {
                    Object[] row = (Object[]) obj;
                    return new ViewStats(
                            Objects.requireNonNull((String) row[0], "app cannot be null"),
                            Objects.requireNonNull((String) row[1], "uri cannot be null"),
                            Optional.ofNullable((Number) row[2]).map(Number::longValue).orElse(0L)
                    );
                })
                .collect(Collectors.toList());
    }
}
