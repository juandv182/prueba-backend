package fastglp.controller.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fastglp.Algorithm.ACO.ACOAlgorithm;
import fastglp.model.Bloqueo;
import fastglp.model.Ciudad;
import fastglp.model.DistanceGraph;
import fastglp.model.Pedido;
import fastglp.service.*;
import fastglp.utils.PoblarBD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Date;

@Controller
public class DayController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private CiudadService ciudadService;
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private BloqueoService bloqueoService;
    @Autowired
    private CamionService camionService;
    @Autowired
    private DistanceGraphService distanceGraphService;
    @Autowired
    private AlmacenService almacenService;
    private Ciudad ciudad;
    private String ciudadJson="2 de enero de 2023";
    private final ObjectMapper mapper = new ObjectMapper();
    private DistanceGraph distanceGraph;
    @Autowired
    private PoblarBD poblarBD;

    @MessageMapping("/execute-day")
    public void broadcastDay() {
        System.out.println("Enviando simulacion dia a dia");
        messagingTemplate.convertAndSend("/response/response-day", ciudadJson);
    }

    public void execute(){
        System.out.println("Ejecucion programada");
        Date fin= new Date();
        Date inicio= new Date(fin.getTime()-300000);
        if(ciudad==null){
            ciudad=ciudadService.buscarPorId(1L);
            if(ciudad==null){
                System.out.println("No se encontró la ciudad");
                poblarBD.poblar();
                System.out.println("Se pobló la base de datos, se intentará de nuevo la siguiente ejecución");
                return;
            }
            ciudad.setCamiones(camionService.listarCamiones(ciudad));
            ciudad.setAlmacenes(almacenService.getAlmacenes(ciudad));
            ciudad.setPedidos(new ArrayList<>());
            ciudad.setBloqueos(new ArrayList<>());
            distanceGraph = new DistanceGraph(ciudad, 6.0);
        }
        ciudad.addPedido(pedidoService.listarPedidosPorFecha(inicio,fin,ciudad));
        ArrayList<Pedido>nuevosPedidos = ciudad.getPedidos().stream().filter(p->
                        p.getPorciones()==null || p.getPorciones().isEmpty() ||
                                p.getPorciones().stream()
                                        .anyMatch(pp->pp.getFechaEntrega()==null||
                                                pp.getFechaEntrega().after(fin)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setPedidos(nuevosPedidos);
        ArrayList<Bloqueo>nuevosBloqueos= ciudad.getBloqueos().stream().filter(b->
                        !b.getFechaFin().before(fin))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setBloqueos(nuevosBloqueos);
        if(!distanceGraph.valid(fin)){
            ciudad.addBloqueo(bloqueoService.listarBloqueosPorFecha(fin,new Date(fin.getTime()+43200000L),ciudad));
            distanceGraph = distanceGraphService.buildOrGetDistanceGraph(distanceGraph,fin,false);
        }
        try {
            ACOAlgorithm.optimizar(ciudad, fin, 5, 1, 3, 10, 50, true,
                    distanceGraph);
            ciudadJson=mapper.writeValueAsString(ciudad.getClearCity(fin));
            ciudad.getDelete().clear();
            ciudad.getUpdate().clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        broadcastDay();
    }
}
