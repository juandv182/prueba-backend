package fastglp.service;

import fastglp.model.Ciudad;
import fastglp.repository.CiudadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CiudadService {
    @Autowired
    private CiudadRepository ciudadRepository;

    @Transactional
    public void guardar(Ciudad ciudad) {
        ciudadRepository.save(ciudad);
    }

    public Ciudad buscarPorId(Long id) {
        return ciudadRepository.findById(id).orElse(null);
    }

    public boolean existsById(Long id) {
        return ciudadRepository.existsById(id);
    }
    public void eliminar(Long id){
        ciudadRepository.deleteById(id);
    }
}
