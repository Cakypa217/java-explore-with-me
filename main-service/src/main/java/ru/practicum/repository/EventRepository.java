package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.entity.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByInitiatorIdAndId(Long userId, Long eventId);

    Boolean existsByCategoryId(Long categoryId);

    @Query(value = """
            SELECT e.* FROM events e
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%'))
            OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
            AND (COALESCE(:categories) IS NULL OR e.category_id IN (:categories))
            AND (:paid IS NULL OR e.paid = :paid)
            AND (e.event_date BETWEEN :rangeStart AND COALESCE(:rangeEnd, e.event_date))
            AND (:onlyAvailable = false OR (e.participant_limit = 0 OR e.confirmed_requests < e.participant_limit))
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Event> findAllByFilters(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("from") Integer from,
            @Param("size") Integer size);

    @Query(value = """
            SELECT DISTINCT e.* FROM events e
            LEFT JOIN categories c ON c.id = e.category_id
            LEFT JOIN users u ON u.id = e.initiator_id
            WHERE (COALESCE(:users) IS NULL OR e.initiator_id IN (:users))
            AND (COALESCE(:states) IS NULL OR e.state IN (:states))
            AND (COALESCE(:categories) IS NULL OR e.category_id IN (:categories))
            AND (e.event_date BETWEEN :rangeStart AND :rangeEnd)
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Event> findAllByAdmin(
            @Param("users") List<Long> users,
            @Param("states") List<String> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("from") Integer from,
            @Param("size") Integer size);
}