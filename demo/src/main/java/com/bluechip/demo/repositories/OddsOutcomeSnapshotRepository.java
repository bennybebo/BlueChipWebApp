package com.bluechip.demo.repositories;

import com.bluechip.demo.model.OddsOutcomeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OddsOutcomeSnapshotRepository extends JpaRepository<OddsOutcomeSnapshot, Long> {
    void deleteBySportKeyAndMarketType(String sportKey, String marketType);

    List<OddsOutcomeSnapshot> findByEdgeNotNullAndEdgeGreaterThanOrderByEdgeDesc(double edgeThreshold);
}
