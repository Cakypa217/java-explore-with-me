package ru.practicum.service;

import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;

import java.util.List;

public interface StatsService {
    void recordHit(EndpointHit hit);

    List<ViewStats> calculateViews(ViewStatsRequest request);
}
