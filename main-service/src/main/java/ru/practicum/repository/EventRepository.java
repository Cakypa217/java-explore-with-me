package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            SELECT e FROM Event e
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%'))
            OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND (e.eventDate BETWEEN :rangeStart AND COALESCE(:rangeEnd, e.eventDate))
            AND (:onlyAvailable = false OR (e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit))
            """)
    List<Event> findAllByFilters(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 @Param("onlyAvailable") Boolean onlyAvailable,
                                 Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE (:users IS NULL OR e.initiator.id IN :users)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (e.eventDate BETWEEN :rangeStart AND :rangeEnd)
            """)
    List<Event> findAllByAdmin(@Param("users") List<Long> users,
                               @Param("states") List<String> states,
                               @Param("categories") List<Long> categories,
                               @Param("rangeStart") LocalDateTime rangeStart,
                               @Param("rangeEnd") LocalDateTime rangeEnd,
                               Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.confirmedRequests = e.confirmedRequests + 1 WHERE e.id = :eventId")
    void incrementConfirmedRequests(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE Event e SET e.confirmedRequests = e.confirmedRequests - 1 WHERE e.id = :eventId AND e.confirmedRequests > 0")
    void decrementConfirmedRequests(@Param("eventId") Long eventId);

//    @Query("""
//        SELECT e FROM Event e
//        WHERE (:users IS NULL OR e.initiator.id IN :users OR :users = '[]')
//        AND (:states IS NULL OR e.state IN :states OR :states = '[]')
//        AND (:categories IS NULL OR e.category.id IN :categories OR :categories = '[]')
//        AND (e.eventDate BETWEEN :rangeStart AND :rangeEnd)
//        """)
//    List<Event> findAllByAdmin(@Param("users") List<Long> users,
//                               @Param("states") List<String> states,
//                               @Param("categories") List<Long> categories,
//                               @Param("rangeStart") LocalDateTime rangeStart,
//                               @Param("rangeEnd") LocalDateTime rangeEnd,
//                               Pageable pageable);
}