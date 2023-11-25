package fastglp.service;

import fastglp.model.Almacen;
import fastglp.model.Ciudad;
import fastglp.repository.AlmacenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AlmacenService {
    @Autowired
    private AlmacenRepository almacenRepository;
    public ArrayList<Almacen> getAlmacenes(Ciudad ciudad){
        ArrayList<Almacen> almacenes = new ArrayList<>(almacenRepository.findByCiudadId(ciudad.getId()));
        almacenes.forEach(a->{
            a.setCiudad(ciudad);
            a.setRegistros(new ArrayList<>());
        });
        return almacenes;
    }
}
