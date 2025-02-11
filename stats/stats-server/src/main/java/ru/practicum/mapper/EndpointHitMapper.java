package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.EndpointHit;
import ru.practicum.entity.EndpointHitEntity;

@Component
public class EndpointHitMapper {
    public EndpointHitEntity toEntity(EndpointHit dto) {
        return EndpointHitEntity.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public EndpointHit toDto(EndpointHitEntity entity) {
        return EndpointHit.builder()
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
