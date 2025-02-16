package ru.practicum.mapper;

import ru.practicum.exception.ForbiddenException;
import ru.practicum.model.dto.event.UpdateEventUserRequest;
import ru.practicum.model.entity.Category;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.Location;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;

public class EventUpdateMapper {

    public Event updateEventFromDto(UpdateEventUserRequest updateEventUserRequest, Event event, Category category) {
        if (updateEventUserRequest.getEventDate() != null) {
            if (updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ForbiddenException("Время события должно быть не раньше чем через 2 часа.");
            }
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getCategory() != null) {
            event.setCategory(category);
        }
        if (updateEventUserRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventUserRequest.getAnnotation());
        }
        if (updateEventUserRequest.getDescription() != null) {
            event.setDescription(updateEventUserRequest.getDescription());
        }
        if (updateEventUserRequest.getLocation() != null) {
            Location locationEntity = new Location(
                    updateEventUserRequest.getLocation().getLat(),
                    updateEventUserRequest.getLocation().getLon()
            );
            event.setLocation(locationEntity);
        }
        if (updateEventUserRequest.getPaid() != null) {
            event.setPaid(updateEventUserRequest.getPaid());
        }
        if (updateEventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventUserRequest.getParticipantLimit());
        }
        if (updateEventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventUserRequest.getRequestModeration());
        }
        if (updateEventUserRequest.getStateAction() != null) {
            event.setState(EventState.valueOf(updateEventUserRequest.getStateAction().name()));
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
        }
        return event;
    }
}