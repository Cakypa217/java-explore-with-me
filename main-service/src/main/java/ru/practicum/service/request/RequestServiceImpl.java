package ru.practicum.service.request;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.dto.participation.ParticipationRequestDto;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.ParticipationRequest;
import ru.practicum.model.entity.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.ParticipationStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.service.event.interfaces.PrivateEventService;
import ru.practicum.service.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final PrivateEventService privateEventService;
    private final UserService userService;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto createRequest(long userId, long eventId) {
        log.info("Запрос на участие в событии с id: {} от пользователя с id: {}", eventId, userId);

        Event event = privateEventService.findById(eventId);
        User user = userService.findById(userId);
        validateForCreate(userId, event);

        boolean needModeration = Boolean.TRUE.equals(event.getRequestModeration());
        log.info("Модерация нужна: {}, Лимит участников: {}", needModeration, event.getParticipantLimit());

        ParticipationRequest participationRequest = new ParticipationRequest();
        participationRequest.setEvent(event);
        participationRequest.setRequester(user);

        if (!needModeration || event.getParticipantLimit() == 0) {
            participationRequest.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            participationRequest.setStatus(ParticipationStatus.PENDING);
        }

        participationRequest.setCreated(LocalDateTime.now());

        log.info("Статус перед сохранением: {}", participationRequest.getStatus());

        ParticipationRequest savedRequest = requestRepository.save(participationRequest);
        ParticipationRequestDto participationRequestDto = requestMapper.toParticipationRequestDto(savedRequest);

        log.info("Создан запрос на участие {}", participationRequestDto);
        return participationRequestDto;
    }

    @Override
    public List<ParticipationRequestDto> getRequests(long userId) {
        log.info("Запрос на получение заявок на участия пользователя с id = {}", userId);

        userService.findById(userId);
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        List<ParticipationRequestDto> requestsDto = requests.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();

        log.info("Получен список заявок на участие размером: {}", requestsDto.size());
        return requestsDto;
    }

    @Override
    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        log.info("Получен запрос на отмену заявки с id = {} пользователя с id = {}", requestId, userId);

        ParticipationRequest request = findById(requestId);
        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Пользователь с id = " + userId +
                    " не может отменить запрос с id = " + requestId + ", так как он ему не принадлежит.");
        }

        if (request.getStatus().equals(ParticipationStatus.CONFIRMED)) {
            Event event = request.getEvent();
            if (event.getConfirmedRequests() > 0) {
                event.setConfirmedRequests(event.getConfirmedRequests() - 1);
                eventRepository.save(event);
            }
        }

        request.setStatus(ParticipationStatus.CANCELED);
        ParticipationRequest savedRequest = requestRepository.save(request);
        ParticipationRequestDto participationRequestDto = requestMapper.toParticipationRequestDto(savedRequest);

        log.info("Запрос с id = {} отменен. {}", requestId, participationRequestDto);
        return participationRequestDto;
    }

    @Override
    public ParticipationRequest findById(long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Запрос с id " + requestId + " не найден"));
    }

    public void validateForCreate(long userId, Event event) {
        if (requestRepository.findByRequesterIdAndEventId(userId, event.getId()).isPresent()) {
            throw new ConflictException("Повторный запрос на участие.");
        }
        if (event.getInitiator().getId() == userId) {
            throw new ConflictException("Инициатор не может участвовать в своём событии.");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }
        long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие.");
        }
    }
}