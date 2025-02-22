package ru.practicum.service.request;

import ru.practicum.model.dto.participation.ParticipationRequestDto;
import ru.practicum.model.entity.ParticipationRequest;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto createRequest(long userId, long eventId);

    List<ParticipationRequestDto> getRequests(long userId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);

    ParticipationRequest findById(long requestId);
}