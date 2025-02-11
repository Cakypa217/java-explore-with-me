package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStatsRequest;
import ru.practicum.service.StatsService;
import ru.practicum.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsService service;

    @PostMapping(path = "/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@RequestBody EndpointHit hit) {
        service.recordHit(hit);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getStats(@RequestParam String start,
                                                    @RequestParam String end,
                                                    @RequestParam(required = false) List<String> uris,
                                                    @RequestParam(defaultValue = "false") boolean unique) {
        try {
            LocalDateTime startDT = LocalDateTime.parse(start, DTF);
            LocalDateTime endDT = LocalDateTime.parse(end, DTF);

            if (startDT.isAfter(endDT)) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            ViewStatsRequest request = ViewStatsRequest.builder()
                    .start(startDT)
                    .end(endDT)
                    .uris((uris == null || uris.isEmpty()) ? Collections.emptyList() : uris)
                    .unique(unique)
                    .build();

            return ResponseEntity.ok(service.calculateViews(request));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }
}