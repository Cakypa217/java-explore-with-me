package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.practicum.model.dto.event.EventFullDto;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.dto.event.NewEventDto;
import ru.practicum.model.dto.event.UpdateEventAdminRequest;
import ru.practicum.model.entity.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventFullDto toEventFullDto(Event event);

    Event toEvent(NewEventDto newEventDto);

    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    void updateEventFromDto(UpdateEventAdminRequest dto, @MappingTarget Event event);
}