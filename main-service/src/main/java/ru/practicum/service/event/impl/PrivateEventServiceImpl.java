package ru.practicum.service.event.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.EventUpdateMapper;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.dto.event.NewEventDto;
import ru.practicum.model.dto.event.UpdateEventUserRequest;
import ru.practicum.model.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.model.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.model.dto.participation.ParticipationRequestDto;
import ru.practicum.model.entity.Category;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.ParticipationRequest;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.ParticipationStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.service.category.CategoryService;
import ru.practicum.service.event.interfaces.PrivateEventService;
import ru.practicum.service.user.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {
    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventMapper eventMapper;
    private final EventUpdateMapper eventUpdateMapper;

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Получен запрос на создание события {}", newEventDto);

        User user = userService.findById(userId);
        Category category = categoryService.findById(newEventDto.getCategory());

        LocalDateTime now = LocalDateTime.now();
        if (newEventDto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Время события должна быть не раньше чем через 2 часа.");
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setCreatedOn(now);
        event.setPublishedOn(null);
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(savedEvent);

        log.info("Событие {} создано", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, Integer from, Integer size) {
        log.info("Получен запрос на получение событий пользователя с id = {}", userId);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<EventShortDto> eventShortDtos = eventRepository.findAllByInitiatorId(userId, pageRequest)
                .map(eventMapper::toEventShortDto)
                .getContent();

        log.info("Получен список событий пользователя с id = {}, количество: {}", userId, eventShortDtos.size());
        return eventShortDtos;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        log.info("Получен запрос на получение события с id = {} пользователя с id = {}", eventId, userId);

        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие не найдено"));

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);

        log.info("Получен событие: {}", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        log.info("Получен запрос на получение запросов на участие в событии с id = {} пользователя с id = {}",
                eventId, userId);

        Event event = findByInitiator(userId, eventId);

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(event.getId());

        List<ParticipationRequestDto> requestDtos = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов на участие в событии с id = {}", requestDtos.size(), eventId);
        return requestDtos;
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Получен запрос на обновление события с id = {} пользователя с id = {}", eventId, userId);

        Event event = findByInitiator(userId, eventId);
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Обновить можно только ожидающие или отмененные события");
        }
        LocalDateTime now = LocalDateTime.now();
        if (updateEventUserRequest.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Время события должна быть не раньше чем через 2 часа.");
        }

        Category category = categoryService.findById(updateEventUserRequest.getCategory());
        Event updatedEv = eventUpdateMapper.updateEventFromDto(updateEventUserRequest, event, category);
        Event updatedEvent = eventRepository.save(updatedEv);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(updatedEvent);

        log.info("Событие с id = {} обновлено", eventId);
        return eventFullDto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.info("Обновление статусов заявок на участие в событии id={} пользователем id={}", eventId, userId);

        findById(eventId);
        Event event = findByInitiator(userId, eventId);

        List<ParticipationRequest> requests = requestRepository.findAllById(eventRequestStatusUpdateRequest.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!request.getStatus().equals(ParticipationStatus.PENDING)) {
                throw new ConflictException("Можно изменять только заявки в статусе 'PENDING'.");
            }
        }

        if (eventRequestStatusUpdateRequest.getStatus().equals(ParticipationStatus.REJECTED)) {
            return rejectRequests(requests);
        } else if (eventRequestStatusUpdateRequest.getStatus().equals(ParticipationStatus.CONFIRMED)) {
            return confirmRequests(requests, event);
        } else {
            throw new BadRequestException("Некорректный статус заявки: " + eventRequestStatusUpdateRequest.getStatus());
        }
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        requests.forEach(request -> request.setStatus(ParticipationStatus.REJECTED));
        requestRepository.saveAll(requests);

        List<ParticipationRequestDto> rejectedRequestsDto = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();

        log.info("Для события отклонено заявок {}", requests.size());
        return new EventRequestStatusUpdateResult(Collections.emptyList(), rejectedRequestsDto);
    }

    private EventRequestStatusUpdateResult confirmRequests(List<ParticipationRequest> requests, Event event) {
        if (event.getParticipantLimit() == 0) {
            requests.forEach(request -> request.setStatus(ParticipationStatus.CONFIRMED));
            requestRepository.saveAll(requests);

            List<ParticipationRequestDto> confirmedDtos = requests.stream()
                    .map(requestMapper::toParticipationRequestDto)
                    .toList();

            log.info("Подтверждены все заявки (лимит участников = 0) для события id={}", event.getId());
            return new EventRequestStatusUpdateResult(confirmedDtos, Collections.emptyList());
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), "CONFIRMED");
        long availableSlots = event.getParticipantLimit() - confirmedCount;


        if (availableSlots <= 0) {
            throw new ConflictException("Лимит участников исчерпан, нельзя подтвердить больше заявок.");
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (availableSlots > 0) {
                request.setStatus(ParticipationStatus.CONFIRMED);
                confirmedRequests.add(request);
                availableSlots--;
            } else {
                request.setStatus(ParticipationStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        requestRepository.saveAll(requests);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(
                confirmedRequests.stream().map(requestMapper::toParticipationRequestDto).toList(),
                rejectedRequests.stream().map(requestMapper::toParticipationRequestDto).toList());

        log.info("Подтверждено {} заявок, отклонено {} для события id={}",
                confirmedRequests.size(), rejectedRequests.size(), event.getId());

        return result;
    }

    @Override
    public Event findByInitiator(Long userId, Long eventId) {
        return eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие id: " + eventId +
                        " не найденоДля у пользователя " + userId ));
    }

    @Override
    public Event findById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие с id = " + eventId + " не найдено"));
    }
}