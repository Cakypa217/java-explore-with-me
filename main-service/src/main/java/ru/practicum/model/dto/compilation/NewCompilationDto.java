package ru.practicum.model.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    @NotBlank
    @Size(max = 50)
    private String title;

    private Boolean pinned;

    private List<Long> events;
}