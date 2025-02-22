package ru.practicum.service.event.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.enums.EventSort;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventService {

    EventFullDto getEventById(Long id, HttpServletRequest request);

    List<EventShortDto> getAll(String text, List<Long> categories, Boolean paid,
                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                               Boolean onlyAvailable, EventSort sort, Integer from, Integer size,
                               HttpServletRequest request);
}