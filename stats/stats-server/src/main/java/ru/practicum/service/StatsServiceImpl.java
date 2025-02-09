package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.repository.StatsRepository;

import java.util.List;

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
        if (request.getUris() == null || request.getUris().isEmpty()) {
            return repository.findViewsStats(
                    request.getStart(),
                    request.getEnd(),
                    request.isUnique()
            );
        } else {
            return repository.findViewsStatsWithUri(
                    request.getStart(),
                    request.getEnd(),
                    request.getUris(),
                    request.isUnique()
            );
        }
    }
}
