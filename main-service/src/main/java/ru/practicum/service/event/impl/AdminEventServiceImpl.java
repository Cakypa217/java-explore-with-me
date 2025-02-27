package ru.practicum.service.event.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.ViewStats;
import ru.practicum.ViewStatsRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CustomEventMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.UpdateEventAdminRequest;
import ru.practicum.model.entity.Category;
import ru.practicum.model.entity.Event;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.service.category.CategoryService;
import ru.practicum.service.event.interfaces.AdminEventService;
import ru.practicum.service.event.interfaces.PrivateEventService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventServiceImpl implements AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final RequestRepository requestRepository;
    private final PrivateEventService privateEventService;
    private final EventMapper eventMapper;
    private final CustomEventMapper customEventMapper;
    private final StatsClient statsClient;


    @Override
    public List<EventFullDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        log.info("Получение событий администратором с критериями: " +
                        "users: {}, states: {}, categories: {}, rangeStart: {}, rangeEnd: {}, from: {}, size: {}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now().minusYears(1);
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(5);
        }
        if (users == null) {
            users = Collections.emptyList();
        }
        if (states == null) {
            states = Collections.emptyList();
        }
        if (categories == null) {
            categories = Collections.emptyList();
        }

        List<EventState> eventStates = states.stream()
                .map(EventState::valueOf)
                .toList();

        List<Event> events = eventRepository.findAllByAdmin(users, eventStates.stream()
                .map(EventState::name)
                .collect(Collectors.toList()), categories, rangeStart, rangeEnd, from, size);

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

        List<EventFullDto> result = events.stream()
                .peek(event -> {
                    long views = viewsMap.getOrDefault("/events/" + event.getId(), 0L);
                    long confirmed = confirmedRequests.getOrDefault(event.getId(), 0L);

                    event.setViews(views);
                    event.setConfirmedRequests(confirmed);
                })
                .map(eventMapper::toEventFullDto)
                .toList();
        log.info("Администратором получено {} событий", result.size());
        return result;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Администратор обновляет событие {}, на {}", eventId, updateRequest);

        Event event = privateEventService.findById(eventId);

        if (updateRequest.getEventDate() != null) {
            LocalDateTime eventDate = updateRequest.getEventDate();
            if (event.getState() == EventState.PUBLISHED && eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }
        }

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Нельзя опубликовать событие," +
                                " так как оно не в статусе ожидания публикации");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить событие, так как оно уже опубликовано");
                    }
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new IllegalArgumentException("Недопустимое действие: " + updateRequest.getStateAction());
            }
        } else {
            event.setState(EventState.PUBLISHED);
        }

        Category category = null;
        if (updateRequest.getCategory() != null) {
            category = categoryService.findById(updateRequest.getCategory());
        }
        customEventMapper.adminUpdateEvent(updateRequest, event, category);
        event = eventRepository.save(event);
        EventFullDto result = eventMapper.toEventFullDto(event);

        log.info("Администратор обновил событие {}", result);
        return result;
    }
}