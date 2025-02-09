package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.repository.StatsRepository;

import java.util.List;
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
        List<Object[]> rawStats = repository.findViewStats(
                request.getStart(),
                request.getEnd(),
                request.getUris().isEmpty() ? null : request.getUris(),
                request.isUnique()
        );
        return rawStats.stream()
                .map(row -> new ViewStats(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }
}
