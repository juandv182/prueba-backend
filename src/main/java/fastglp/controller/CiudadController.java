package fastglp.controller;

import fastglp.Algorithm.ACO.ACOAlgorithm;
import fastglp.model.*;
import fastglp.repository.CiudadRepository;
import fastglp.service.CiudadService;
import fastglp.service.DistanceGraphService;
import fastglp.model.DistanceGraph;
import fastglp.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/ciudad")
public class CiudadController {
    private Ciudad mockCiudad;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH");

    @Autowired
    private DistanceGraphService distanceGraphService;
    private CiudadService ciudadService;
    @GetMapping(path = "", produces = "application/json")
    public Ciudad obtenerCiudad() {
        Date fechaInicio = Utils.parseFecha("01/09/2023 00");
        while (true){
            if(ejecutar1Dia()){
                break;
            }
        }
        return mockCiudad.getClearCity(fechaInicio);
    }
    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<Ciudad> obtenerCiudadPorId(@PathVariable long id) {
        log.info("Obtener pedido por ID: " + id);
        var ciudadOptional = ciudadService.buscarPorId(id);
        if (ciudadOptional != null) {
            return new ResponseEntity<>(ciudadOptional, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    public boolean ejecutar1Dia(){
        Date fechaInicio = Utils.parseFecha("01/09/2023 00");
        //7 dias
        Date fechaFin = new Date(fechaInicio.getTime() + 86400000L);
        mockCiudad = Utils.createMockCiudad(9,2023);
        DistanceGraph dg = new DistanceGraph(mockCiudad, 24.0);
        int i=0;
        while (fechaInicio.before(fechaFin)) {
            // Ejecuta el algoritmo ACO aquí
            if(i%70==0){
                System.out.println("Siendo: "+fechaInicio);
            }
            if(!ACOAlgorithm.optimizar(mockCiudad, fechaInicio, 5, 1, 2, 2, 0, true,
                    (dg=distanceGraphService.buildOrGetDistanceGraph(dg,fechaInicio,true)))){
                return false;
            }
            fechaInicio = new Date(fechaInicio.getTime() + 300000L);
            i++;
        }
        return true;
    }
}
