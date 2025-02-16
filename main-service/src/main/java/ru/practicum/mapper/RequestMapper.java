package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.model.dto.participation.ParticipationRequestDto;
import ru.practicum.model.entity.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    ParticipationRequestDto toParticipationRequestDto(ParticipationRequest participationRequest);

}