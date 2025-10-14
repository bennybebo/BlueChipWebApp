package com.bluechip.demo.repositories;

import com.bluechip.demo.model.OddsOutcomeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OddsOutcomeSnapshotRepository extends JpaRepository<OddsOutcomeSnapshot, Long> {
    void deleteBySportKeyAndMarketType(String sportKey, String marketType);

    List<OddsOutcomeSnapshot> findByEdgeNotNullAndEdgeGreaterThanOrderByEdgeDesc(double edgeThreshold);

    @Query("select max(s.refreshedAt) from OddsOutcomeSnapshot s where s.sportKey = :sportKey and s.marketType = :marketType")
    Optional<Instant> findMostRecentRefresh(@Param("sportKey") String sportKey,
                                            @Param("marketType") String marketType);

}
