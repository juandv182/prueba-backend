package fastglp.repository;

import fastglp.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long>{
    @Query("SELECT p FROM Pedido p WHERE p.ciudad.id=:idCiudad AND p.fechaSolicitud BETWEEN :inicio AND :fin")
    List<Pedido>findByFechaSolicitudBetween(@Param("inicio") Date inicio, @Param("fin") Date fin,
                                            @Param("idCiudad") Long idCiudad);
}
