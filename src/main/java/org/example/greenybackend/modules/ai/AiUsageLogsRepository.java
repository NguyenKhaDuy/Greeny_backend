package org.example.greenybackend.modules.ai;

import java.time.LocalDateTime;
import org.example.greenybackend.domain.entity.AiUsageLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageLogsRepository extends JpaRepository<AiUsageLogs, String> {

    @Query("""
            select count(log)
            from AiUsageLogs log
            where lower(log.provider) = lower(:provider)
              and log.success = true
              and log.createdAt >= :start
              and log.createdAt < :end
            """)
    long countSuccessfulProviderCallsToday(
            @Param("provider") String provider,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select coalesce(sum(coalesce(log.promptTokens, 0) + coalesce(log.completionTokens, 0)), 0)
            from AiUsageLogs log
            where lower(log.provider) = lower(:provider)
              and log.success = true
              and log.createdAt >= :start
              and log.createdAt < :end
            """)
    long sumSuccessfulProviderTokensToday(
            @Param("provider") String provider,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
