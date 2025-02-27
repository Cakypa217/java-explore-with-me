package ru.practicum.service.event.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.CustomEventMapper;
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
@Transactional(readOnly = true)
public class PrivateEventServiceImpl implements PrivateEventService {
    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventMapper eventMapper;
    private final CustomEventMapper customEventMapper;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Запрос на создание события {} пользователем под id: {}", newEventDto, userId);

        User user = userService.findById(userId);
        Category category = categoryService.findById(newEventDto.getCategory());

        LocalDateTime now = LocalDateTime.now();
        if (newEventDto.getEventDate().isBefore(now.plusHours(2))) {
            throw new BadRequestException("Время события должна быть не раньше чем через 2 часа.");
        }

        Event event = customEventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setCreatedOn(now);
        event.setPublishedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(savedEvent);

        log.info("Создано событие {}", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, Integer from, Integer size) {
        log.info("Запрос на получение событий пользователя с id = {} и параметрами from: {}, size: {}",
                userId, from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<EventShortDto> eventShortDtos = eventRepository.findAllByInitiatorId(userId, pageRequest)
                .map(eventMapper::toEventShortDto)
                .getContent();

        log.info("Получен список событий пользователя с id = {}, количеством: {}", userId, eventShortDtos.size());
        return eventShortDtos;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        log.info("Запрос на получение события с id = {} пользователя с id = {}", eventId, userId);

        Event event = eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие не найдено"));

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);

        log.info("Получено событие: {}", eventFullDto);
        return eventFullDto;
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии с id = {} пользователя с id = {}", eventId, userId);

        Event event = findByInitiator(userId, eventId);

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(event.getId());

        List<ParticipationRequestDto> requestDtos = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов на участие в событии с id: {}", requestDtos.size(), eventId);
        return requestDtos;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Запрос на обновление события с id: {} пользователя с id: {} на {}",
                eventId, userId, updateEventUserRequest);

        Event event = findByInitiator(userId, eventId);
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Обновить можно только ожидающие или отмененные события");
        }
        if (updateEventUserRequest.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (updateEventUserRequest.getEventDate().isBefore(now.plusHours(2))) {
                throw new BadRequestException("Время события должна быть не раньше чем через 2 часа.");
            }
        }

        Event updatedEv = customEventMapper.userApdateEvent(updateEventUserRequest, event);

        if (updateEventUserRequest.getCategory() != null) {
            Category category = categoryService.findById(updateEventUserRequest.getCategory());
            updatedEv.setCategory(category);
        }

        Event updatedEvent = eventRepository.save(updatedEv);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(updatedEvent);

        log.info("Событие с id: {} обновлено на {} ", eventId, eventFullDto);
        return eventFullDto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.info("Обновление статусов заявок на участие в событии id: {} пользователем id: {}", eventId, userId);

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
            long confirmedCount = event.getConfirmedRequests().intValue();
            long participantLimit = event.getParticipantLimit();

            if (participantLimit > 0 && confirmedCount >= participantLimit) {
                throw new ConflictException("Достигнут лимит участников для этого события.");
            }

            if (participantLimit > 0 && confirmedCount + requests.size() > participantLimit) {
                throw new ConflictException("Невозможно подтвердить заявки, превышается лимит участников.");
            }

            EventRequestStatusUpdateResult result = confirmRequests(requests, event);
            event.setConfirmedRequests(event.getConfirmedRequests() + requests.size());
            eventRepository.save(event);

            return result;
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

            log.info("Подтверждены все заявки (лимит участников = 0) для события id: {}", event.getId());
            return new EventRequestStatusUpdateResult(confirmedDtos, Collections.emptyList());
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.CONFIRMED);
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

        log.info("Подтверждено {} заявок, отклонено {} для события id: {}",
                confirmedRequests.size(), rejectedRequests.size(), event.getId());

        return result;
    }

    @Override
    public Event findByInitiator(Long userId, Long eventId) {
        return eventRepository.findByInitiatorIdAndId(userId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие id: " + eventId +
                        " не найденоДля у пользователя " + userId));
    }

    @Override
    public Event findById(Long eventId) {
        log.info("Получен запрос на получение события с id = {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие с id = " + eventId + " не найдено"));
    }
}