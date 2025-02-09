package ru.practicum;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class ViewStats {
    private String app;
    private String uri;
    private Long hits;

}
