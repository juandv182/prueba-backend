package fastglp.service;

import fastglp.model.Camion;
import fastglp.model.Ciudad;
import fastglp.repository.CamionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CamionService {
    @Autowired
    private CamionRepository camionRepository;

    public ArrayList<Camion> listarCamiones(Ciudad ciudad) {
        ArrayList<Camion> camiones = new ArrayList<>(camionRepository.findByCiudadId(ciudad.getId()));
        camiones.forEach(c->{
            c.setRuta(new ArrayList<>());
            c.setCiudad(ciudad);
        });
        return camiones;
    }
}
