package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.model.dto.compilation.CompilationDto;
import ru.practicum.model.dto.compilation.NewCompilationDto;
import ru.practicum.model.dto.compilation.UpdateCompilationRequest;
import ru.practicum.model.entity.Compilation;
import ru.practicum.model.entity.Event;
import ru.practicum.repository.EventRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "eventIds", qualifiedByName = "mapEvents")
    Compilation toCompilation(NewCompilationDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true) // Обновляем в сервисе
    void updateCompilationFromDto(UpdateCompilationRequest dto, @MappingTarget Compilation compilation);

    @Named("mapEvents")
    default Set<Event> mapEvents(List<Long> eventIds, @Context EventRepository eventRepository) {
        return eventIds == null ? Collections.emptySet() : new HashSet<>(eventRepository.findAllById(eventIds));
    }

    CompilationDto toCompilationDto(Compilation compilation);
}