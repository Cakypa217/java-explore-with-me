package ru.practicum.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String annotation;

    @Column(nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Transient
    private long confirmedRequests;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    private LocalDateTime publishedOn;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private Boolean paid;

    private Long participantLimit;

    private Boolean requestModeration = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventState state;

    private Long views = 0L;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}