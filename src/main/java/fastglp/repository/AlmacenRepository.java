package fastglp.repository;

import fastglp.model.Almacen;
import fastglp.model.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
    List<Almacen> findByCiudadId(Long idCiudad);
}
