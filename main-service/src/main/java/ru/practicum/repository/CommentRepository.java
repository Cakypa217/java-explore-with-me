package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.entity.Comment;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdOrderByCreatedAsc(Long eventId);

    Optional<Comment> findByIdAndUserId(Long commentId, Long userId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.event.id = :eventId")
    void deleteCommentsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT c FROM Comment c WHERE c.event.id IN :eventIds")
    List<Comment> getCommentsByEventIds(@Param("eventIds") List<Long> eventIds);
}