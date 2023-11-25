package fastglp.repository;

import fastglp.model.DistanceGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface DistanceGraphRepository extends JpaRepository<DistanceGraph, Long> {
    Optional<DistanceGraph> findByBuildAndInterval(Date build, long interval);
}
