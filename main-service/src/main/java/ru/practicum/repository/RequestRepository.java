package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.entity.ParticipationRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, String status);

    @Query("""
            SELECT r.event.id, COUNT(r) 
            FROM ParticipationRequest r 
            WHERE r.status = 'CONFIRMED' 
            AND r.event.id IN :eventIds
            GROUP BY r.event.id
            """)
    Map<Long, Long> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);
}