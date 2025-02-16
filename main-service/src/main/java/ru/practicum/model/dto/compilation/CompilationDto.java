package ru.practicum.model.dto.compilation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.model.dto.event.EventShortDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {

    @NotNull
    private Long id;

    @NotEmpty
    private String title;

    @NotNull
    private Boolean pinned;

    private List<EventShortDto> events;
}