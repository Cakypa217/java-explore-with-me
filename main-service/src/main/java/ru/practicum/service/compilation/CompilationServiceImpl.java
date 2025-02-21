package ru.practicum.service.compilation;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.dto.compilation.CompilationDto;
import ru.practicum.model.dto.compilation.NewCompilationDto;
import ru.practicum.model.dto.compilation.UpdateCompilationRequest;
import ru.practicum.model.dto.event.EventShortDto;
import ru.practicum.model.entity.Compilation;
import ru.practicum.model.entity.Event;
import ru.practicum.repository.CompilationsRepository;
import ru.practicum.repository.EventRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationsRepository compilationsRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Запрос на создание подборки: {}", newCompilationDto);

        Set<Event> events = newCompilationDto.getEvents() == null
                ? Collections.emptySet()
                : new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));

        Set<EventShortDto> eventShortDto = events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toSet());


        Compilation compilation = compilationMapper.toCompilation(newCompilationDto, events);
        Compilation savedCompilation = compilationsRepository.save(compilation);
        CompilationDto savedCompilationDto = compilationMapper.toCompilationDto(savedCompilation, eventShortDto);

        log.info("Создана подборка: {}", savedCompilationDto);
        return savedCompilationDto;
    }

    @Override
    public void deleteCompilation(Long compId) {
        log.info("Запрос на удаление подборки: {}", compId);
        compilationsRepository.deleteById(compId);
        log.info("Подборка с id: {} удалена", compId);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Запрос на обновление подборки c id: {} , на {}", compId, updateCompilationRequest);

        Compilation compilation = compilationsRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Подборка с id:" + compId + " не найдена"));


        Compilation updateCompilation = compilationMapper
                .updateCompilationFromDto(updateCompilationRequest, compilation);

        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            compilation.setEvents(new HashSet<>(events));
        }

        Set<EventShortDto> eventShortDto = compilation.getEvents().stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toSet());

        Compilation updatedCompilation = compilationsRepository.save(updateCompilation);
        CompilationDto updatedCompilationDto = compilationMapper.toCompilationDto(updatedCompilation, eventShortDto);

        log.info("Подборка обновлена: {}", updatedCompilation);
        return updatedCompilationDto;
    }

    @Override
    public List<CompilationDto> getAllcompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Запрос на получение подборок");

        Page<Compilation> compilationsPage;
        Pageable pageable = PageRequest.of(from / size, size);

        if (pinned != null) {
            compilationsPage = compilationsRepository.findAllByPinned(pinned, pageable);
        } else {
            compilationsPage = compilationsRepository.findAll(pageable);
        }

        List<CompilationDto> compilationD = compilationsPage.getContent().stream()
                .map(compilation -> {
                    Set<EventShortDto> eventShort = compilation.getEvents().stream()
                            .map(eventMapper::toEventShortDto)
                            .collect(Collectors.toSet());
                    return compilationMapper.toCompilationDto(compilation, eventShort);
                })
                .toList();

        log.info("Список подборок: {}", compilationD);
        return compilationD;
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Запрос на получение подборки по id: {}", compId);

        Compilation compilation = compilationsRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Подборка с id:" + compId + " не найдена"));

        Set<EventShortDto> eventShortDto = compilation.getEvents().stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toSet());

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation, eventShortDto);

        log.info("Найденная подборка: {}", compilationDto);
        return compilationDto;
    }
}