package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.exception.ForbiddenException;
import ru.practicum.model.dto.event.NewEventDto;
import ru.practicum.model.dto.event.UpdateEventAdminRequest;
import ru.practicum.model.dto.event.UpdateEventUserRequest;
import ru.practicum.model.entity.Category;
import ru.practicum.model.entity.Event;
import ru.practicum.model.entity.Location;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;

import static ru.practicum.model.enums.StateAction.CANCEL_REVIEW;
import static ru.practicum.model.enums.StateAction.SEND_TO_REVIEW;

@Component
public class ImplEventMapper {

    public Event toEvent(NewEventDto newEventDto) {
        Event event = new Event();
        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());
        event.setLocation(new Location(newEventDto.getLocation().getLat(), newEventDto.getLocation().getLon()));
        event.setPaid(newEventDto.getPaid());
        event.setParticipantLimit(newEventDto.getParticipantLimit() == null ? 0 : newEventDto.getParticipantLimit());
        event.setRequestModeration(newEventDto.getRequestModeration());
        event.setTitle(newEventDto.getTitle());
        event.setState(EventState.PENDING);
        return event;
    }

    public Event userApdateEvent(UpdateEventUserRequest updateEventUserRequest, Event event) {
        if (updateEventUserRequest.getEventDate() != null) {
            if (updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ForbiddenException("Время события должно быть не раньше чем через 2 часа.");
            }
            event.setEventDate(updateEventUserRequest.getEventDate());
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
            switch (updateEventUserRequest.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
        if (updateEventUserRequest.getTitle() != null) {
            event.setTitle(updateEventUserRequest.getTitle());
        }
        return event;
    }

    public Event adminUpdateEvent(UpdateEventAdminRequest updateEventAdminRequest, Event event, Category category) {
        if (updateEventAdminRequest.getAnnotation() != null) {
            event.setAnnotation(updateEventAdminRequest.getAnnotation());
        }
        if (updateEventAdminRequest.getCategory() != null) {
            event.setCategory(category);
        }
        if (updateEventAdminRequest.getDescription() != null) {
            event.setDescription(updateEventAdminRequest.getDescription());
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            if (updateEventAdminRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ForbiddenException("Время события должно быть не раньше чем через 2 часа.");
            }
            event.setEventDate(updateEventAdminRequest.getEventDate());
        }
        if (updateEventAdminRequest.getLocation() != null) {
            Location locationEntity = new Location(
                    updateEventAdminRequest.getLocation().getLat(),
                    updateEventAdminRequest.getLocation().getLon()
            );
            event.setLocation(locationEntity);
        }
        if (updateEventAdminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }
        if (updateEventAdminRequest.getTitle() != null) {
            event.setTitle(updateEventAdminRequest.getTitle());
        }
        if (updateEventAdminRequest.getPaid() != null) {
            event.setPaid(updateEventAdminRequest.getPaid());
        }
        return event;
    }
}