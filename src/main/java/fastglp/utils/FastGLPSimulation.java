package fastglp.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fastglp.Algorithm.ACO.ACOAlgorithm;
import fastglp.model.*;
import fastglp.service.DistanceGraphService;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class FastGLPSimulation {
    @Setter @JsonIgnore
    private DistanceGraphService distanceGraphService;
    @JsonIgnore
    private final Ciudad ciudad;
    @Setter
    private String type; //["7days", "collapse"];
    @JsonIgnore
    @Setter
    private DistanceGraph distanceGraph;
    @JsonIgnore @Setter
    private boolean firstTime = true;
    private Date currentTime;
    private final long interval = 300000L;
    @Setter
    private Date fechaInicio;
    @Setter
    private Date fechaFin;
    @Setter
    private boolean last=false;
    private final Estadisticas estadisticas;

    public FastGLPSimulation(Ciudad ciudad,Date fechaInicio,Date fechaFin) {
        this.ciudad = ciudad;
        this.fechaInicio=fechaInicio;
        this.fechaFin=fechaFin;
        this.currentTime = fechaInicio;
        long id=0;
        for(Almacen almacen:ciudad.getAlmacenes()){
            almacen.setId(id++);
        }
        ciudad.setPedidos(ciudad.getPedidos().stream().filter(p->p.getFechaSolicitud().compareTo(fechaInicio)>=0&&
                p.getFechaSolicitud().compareTo(fechaFin)<=0).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        ciudad.setBloqueos(ciudad.getBloqueos().stream().filter(b->b.getFechaInicio().compareTo(fechaInicio)>=0&&
                b.getFechaInicio().compareTo(fechaFin)<=0).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        this.estadisticas = new Estadisticas(ciudad,fechaInicio);
    }

    public boolean optimize(){
        if(type.equals("7days")){
            return optimize7Days();
        }else if(type.equals("collapse")){
            return optimizeCollapse();
        }
        return false;
    }

    private boolean optimize7Days() {
        if(currentTime.compareTo(fechaFin)>0){
            this.last=true;
            return false;
        }
        ArrayList<Pedido>nuevosPedidos = ciudad.getPedidos().stream().filter(p->
                p.getPorciones()==null || p.getPorciones().isEmpty() ||
                        p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||pp.getFechaEntrega().after(currentTime)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setPedidos(nuevosPedidos);
        ArrayList<Bloqueo>nuevosBloqueos= ciudad.getBloqueos().stream().filter(b->
                !b.getFechaFin().before(currentTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setBloqueos(nuevosBloqueos);
        distanceGraph = distanceGraphService.buildOrGetDistanceGraph(distanceGraph,currentTime,true);
        ACOAlgorithm.optimizar(ciudad, currentTime, 5, 1, 3, 4, 40, false,
                distanceGraph);
        estadisticas.decreaseEstadisticas(ciudad.getDelete(),ciudad.getUpdate());
        ciudad.getDelete().clear();
        ciudad.getUpdate().clear();
        estadisticas.addEstadisticas(currentTime);
        currentTime = new Date(currentTime.getTime() + interval);
        return true;
    }


    private boolean optimizeCollapse() {
        return false;
    }

    @JsonProperty("ciudad")
    public Ciudad getCiudad() {
        Ciudad ciudadClear = ciudad.getClearCity(currentTime);
        long inicio = currentTime.getTime();
        long fin = currentTime.getTime()+interval;
        List<Pedido> pedidos = ciudad.getPedidos().stream().filter(p-> (
                between(p.getFechaSolicitud().getTime(),inicio,fin)
                ||p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||between(pp.getFechaEntrega().getTime(),inicio,fin))))
                .toList();
        ciudadClear.setPedidos(pedidos);
        return ciudadClear;
    }

    private boolean between(long fecha, long fechaInicio, long fechaFin){
        return fecha>=fechaInicio && fecha<=fechaFin;
    }
}
