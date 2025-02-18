package ru.practicum.service.event.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.exception.BadRequestException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.dto.client.EndpointHit;
import ru.practicum.model.dto.client.ViewStats;
import ru.practicum.model.dto.client.ViewStatsRequest;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.entity.Event;
import ru.practicum.model.enums.EventSort;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.ParticipationStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.service.event.interfaces.PublicEventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("Получен запрос на получение события с id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new BadRequestException("Событие ещё не опубликовано");
        }

        statsClient.hit(EndpointHit.builder()
                .app("main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());

        List<ViewStats> stats = statsClient.getStats(ViewStatsRequest.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of("/events/" + eventId))
                .unique(true)
                .build());

        long views = stats.isEmpty() ? 0 : stats.getFirst().getHits();

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        eventFullDto.setViews(views);
        eventFullDto.setConfirmedRequests(confirmedRequests);

        log.info("Получено событие {}", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getAll(String text, List<Long> categories, Boolean paid,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                      Boolean onlyAvailable, EventSort sort, Integer from, Integer size,
                                      HttpServletRequest request) {
        log.info("Получен запрос на получение событий");

        statsClient.hit(EndpointHit.builder()
                .app("main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(1);
        }

        List<Event> events = eventRepository.findAllByFilters(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, PageRequest.of(from / size, size));

        if (events.isEmpty()) {
            log.info("По заданным фильтрам события не найдены");
            return Collections.emptyList();
        }

        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
        List<ViewStats> stats = statsClient.getStats(ViewStatsRequest.builder()
                .start(rangeStart)
                .end(rangeEnd)
                .uris(uris)
                .unique(true)
                .build());

        Map<String, Long> viewsMap = Optional.ofNullable(stats)
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));

        Map<Long, Long> confirmedRequests = events.isEmpty()
                ? Collections.emptyMap()
                : requestRepository.countConfirmedRequestsByEventIds(events.stream().map(Event::getId).toList());

        List<EventShortDto> result = events.stream()
                .peek(event -> {
                    long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
                    long confirmed = confirmedRequests.getOrDefault(event.getId(), 0L);

                    event.setViews(views);
                    event.setConfirmedRequests(confirmed);
                })
                .map(eventMapper::toEventShortDto)
                .sorted(sort == EventSort.VIEWS ? Comparator.comparing(EventShortDto::getViews).reversed()
                        : Comparator.comparing(EventShortDto::getEventDate))
                .toList();

        log.info("Найдено {} событий", result.size());
        return result;
    }
}