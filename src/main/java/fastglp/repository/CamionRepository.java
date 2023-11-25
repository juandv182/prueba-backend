package fastglp.repository;

import fastglp.model.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {
    List<Camion> findByCiudadId(Long id);
}