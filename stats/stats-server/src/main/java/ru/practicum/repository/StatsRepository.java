package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.entity.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<EndpointHitEntity, Long> {

    @Query(value = """
            SELECT app, uri,
            CASE WHEN :unique = true THEN COUNT(DISTINCT ip) ELSE COUNT(ip) END AS hits
            FROM endpoint_hit
            WHERE timestamp BETWEEN :start AND :end
            GROUP BY app, uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<Object[]> findViewStatsWithoutUris(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("unique") boolean unique);

    @Query(value = """
            SELECT app, uri,
            CASE WHEN :unique = true THEN COUNT(DISTINCT ip) ELSE COUNT(ip) END AS hits
            FROM endpoint_hit
            WHERE timestamp BETWEEN :start AND :end
            AND uri IN (:uris)
            GROUP BY app, uri
            ORDER BY hits DESC
            """, nativeQuery = true)
    List<Object[]> findViewStatsWithUris(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("uris") List<String> uris,
                                         @Param("unique") boolean unique);
}
