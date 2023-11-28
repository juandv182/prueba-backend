package fastglp.service;

import fastglp.model.Bloqueo;
import fastglp.model.Ciudad;
import fastglp.model.Pedido;
import fastglp.repository.BloqueoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BloqueoService {
    @Autowired
    private BloqueoRepository bloqueoRepository;

    public ArrayList<Bloqueo> listarBloqueosPorFecha(Date fechaInicio, Date fechaFin, Ciudad ciudad) {
        return new ArrayList<>(bloqueoRepository.findByFechaBetween(fechaInicio, fechaFin, ciudad.getId()));
    }
    public void guardarBloqueos(List<Bloqueo> bloqueos) {
        bloqueoRepository.saveAll(bloqueos);
    }

}
