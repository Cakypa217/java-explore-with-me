package ru.practicum.service.event.interfaces;

import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminEventService {

    List<EventFullDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);
}