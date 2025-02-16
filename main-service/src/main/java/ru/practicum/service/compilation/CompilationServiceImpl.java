package ru.practicum.service.compilation;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.dto.compilation.CompilationDto;
import ru.practicum.model.dto.compilation.NewCompilationDto;
import ru.practicum.model.dto.compilation.UpdateCompilationRequest;
import ru.practicum.model.entity.Compilation;
import ru.practicum.model.entity.Event;
import ru.practicum.repository.CompilationsRepository;
import ru.practicum.repository.EventRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationsRepository compilationsRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Запрос на создание подборки: {}", newCompilationDto);

        Set<Event> events = newCompilationDto.getEvents() == null
                ? Collections.emptySet()
                : new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));

        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setPinned(newCompilationDto.getPinned());
        compilation.setEvents(events);

        Compilation savedCompilation = compilationsRepository.save(compilation);
        CompilationDto savedCompilationDto = compilationMapper.toCompilationDto(savedCompilation);


        log.info("Создана подборка: {}", savedCompilationDto);
        return savedCompilationDto;
    }

    @Override
    public void deleteCompilation(Long compId) {
        log.info("Запрос на удаление подборки: {}", compId);
        compilationsRepository.deleteById(compId);
        log.info("Compilation deleted: {}", compId);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Запрос на обновление подборки: {}", compId);

        Compilation compilation = compilationsRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation with id=" + compId + " not found"));

        compilationMapper.updateCompilationFromDto(updateCompilationRequest, compilation);

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        if (updateCompilationRequest.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updateCompilationRequest.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }

        Compilation updatedCompilation = compilationsRepository.save(compilation);
        CompilationDto updatedCompilationDto = compilationMapper.toCompilationDto(updatedCompilation);

        log.info("Обновленная подборка: {}", updatedCompilation);
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
                .map(compilationMapper::toCompilationDto)
                .toList();

        log.info("Список подборок: {}", compilationD);
        return compilationD;
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Запрос на получение подборки по id: {}", compId);

        Compilation compilation = compilationsRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation with id=" + compId + " not found"));

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);

        log.info("Подборка: {}", compilationDto);
        return compilationDto;
    }
}