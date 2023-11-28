package fastglp.service;

import fastglp.repository.DistanceGraphRepository;
import fastglp.model.DistanceGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DistanceGraphService {
    @Autowired
    private DistanceGraphRepository distanceGraphRepository;

    public DistanceGraph buildOrGetDistanceGraph(DistanceGraph distanceGraph, Date init, boolean insertar) {
        if(distanceGraph.valid(init)){
            return distanceGraph;
        }
        distanceGraph.destroy();
        System.out.println("Son las "+init);
        if(insertar){
            Optional<DistanceGraph> existingGraph = distanceGraphRepository.findByBuildAndInterval(init, distanceGraph.getInterval());
            if (existingGraph.isPresent()) {
                // Encuentra un DistanceGraph existente y lo devuelve
                System.out.println("Se encontró un DistanceGraph en bd");
                DistanceGraph graph = existingGraph.get();
                // Aquí podrías actualizar el distanceMap a partir de las distancias almacenadas
                graph.updateDistanceMap();
                graph.setCiudad(distanceGraph.getCiudad());
                return graph;
            }
        }

        // No se encontró un DistanceGraph existente, crea uno nuevo
        System.out.println("No se encontró un DistanceGraph en bd");
        DistanceGraph newGraph = new DistanceGraph(distanceGraph.getCiudad(), distanceGraph.getInterval());
        newGraph.buildGraph(init);
        if(insertar){
            // Persistir el nuevo DistanceGraph en la base de datos
            newGraph.setBuild();
            distanceGraphRepository.save(newGraph);
            //distanceService.saveDistancesInBatch(newGraph.getDistances());
            newGraph.clearBuild();
        }
        return newGraph;

    }

}
