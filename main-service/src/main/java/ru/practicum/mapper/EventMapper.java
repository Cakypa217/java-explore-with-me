package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.entity.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventFullDto toEventFullDto(Event event);

    EventShortDto toEventShortDto(Event event);
}