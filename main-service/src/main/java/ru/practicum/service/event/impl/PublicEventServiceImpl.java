package ru.practicum.service.event.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;
import ru.practicum.exception.BadRequestException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.dto.comment.CommentDto;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.entity.Event;
import ru.practicum.model.enums.EventSort;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.ParticipationStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.service.comment.CommentService;
import ru.practicum.service.event.interfaces.PublicEventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicEventServiceImpl implements PublicEventService {
    private final CommentService commentService;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("Запрос на получение события с id: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new EntityNotFoundException("Событие ещё не опубликовано");
        }

        statsClient.hit(request);

        List<ViewStats> stats = statsClient.getStats(ViewStatsRequest.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of("/events/" + eventId))
                .unique(true)
                .build());


        long views = stats.isEmpty() ? 0 : stats.getFirst().getHits();
        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationStatus.CONFIRMED);

        List<CommentDto> comments = commentService.getCommentsByEvent(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        eventFullDto.setViews(views);
        eventFullDto.setConfirmedRequests(confirmedRequests);
        eventFullDto.setComments(comments);

        log.info("Получено событие {}", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getAll(String text, List<Long> categories, Boolean paid,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                      Boolean onlyAvailable, EventSort sort, Integer from, Integer size,
                                      HttpServletRequest request) {
        log.info("Запрос на получение событий");

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть позже даты окончания");
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(5);
        }

        statsClient.hit(request);

        List<Event> events = eventRepository.findAllByFilters(text, categories, paid,
                rangeStart, rangeEnd, onlyAvailable, from, size);

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
                : requestRepository.countConfirmedRequestsByEventIds(events.stream().map(Event::getId).toList())
                .stream()
                .collect(Collectors.toMap(o -> (Long) o[0], o -> (Long) o[1]));

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