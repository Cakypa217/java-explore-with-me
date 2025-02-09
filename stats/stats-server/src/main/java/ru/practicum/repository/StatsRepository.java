package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ViewStats;
import ru.practicum.entity.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<EndpointHitEntity, Long> {

    @Query("SELECT new ru.practicum.ViewStats(h.app, h.uri, " +
            "COUNT(DISTINCT CASE WHEN :unique = true THEN h.ip ELSE NULL END)) " +
            "FROM EndpointHitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStats> findViewsStats(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("unique") boolean unique);

    @Query("SELECT new ru.practicum.ViewStats(h.app, h.uri, " +
            "COUNT(DISTINCT CASE WHEN :unique = true THEN h.ip ELSE NULL END)) " +
            "FROM EndpointHitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStats> findViewsStatsWithUri(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris,
                                          @Param("unique") boolean unique);
}
