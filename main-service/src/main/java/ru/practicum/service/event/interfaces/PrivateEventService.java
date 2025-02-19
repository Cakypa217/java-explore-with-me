package ru.practicum.service.event.interfaces;

import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.dto.event.NewEventDto;
import ru.practicum.model.dto.event.UpdateEventUserRequest;
import ru.practicum.model.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.model.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.model.dto.participation.ParticipationRequestDto;
import ru.practicum.model.entity.Event;

import java.util.List;

public interface PrivateEventService {

    List<EventShortDto> getEvents(Long userId, Integer from, Integer size);

    EventFullDto getEventById(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequests(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    Event findById(Long eventId);

    Event findByInitiator(Long eventId, Long userId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);
}