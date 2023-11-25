package fastglp.repository;

import fastglp.model.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {
    @Query("SELECT b FROM Bloqueo b WHERE b.ciudad.id= :idCiudad AND b.fechaInicio <= :fin AND b.fechaFin >= :inicio")
    public List<Bloqueo>findByFechaBetween(@Param("inicio") Date inicio, @Param("fin") Date fin,
                                           @Param("idCiudad") Long idCiudad);
}
