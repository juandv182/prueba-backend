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
    @JsonIgnore
    private Estadisticas estadisticas;

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
    }

    public void setType(String type) {
        this.type = type;
        if(type.equals("7days")){
            this.estadisticas = new Estadisticas(ciudad,fechaInicio,"Simulación semanal");
        }else if(type.equals("collapse")){
            this.estadisticas = new Estadisticas(ciudad,fechaInicio,"Simulación colapso");
        }
    }

    // return true if the simulation is over
    public boolean optimize(){
        if(type.equals("7days")){
            return !optimize7Days();
        }else if(type.equals("collapse")){
            return !optimizeCollapse();
        }
        return true;
    }

    private boolean optimize7Days() {
        if(currentTime.compareTo(fechaFin)>0){
            this.last=true;
            return false;
        }
        ArrayList<Pedido> nuevosPedidos = ciudad.getPedidos().stream().filter(p->
                        p.getPorciones()==null || p.getPorciones().isEmpty() ||
                                p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||pp.getFechaEntrega().after(currentTime)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setPedidos(nuevosPedidos);
        ArrayList<Bloqueo>nuevosBloqueos= ciudad.getBloqueos().stream().filter(b->
                        !b.getFechaFin().before(currentTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setBloqueos(nuevosBloqueos);
        distanceGraph = distanceGraphService.buildOrGetDistanceGraph(distanceGraph,currentTime,true);
        if(!ACOAlgorithm.optimizar(ciudad, currentTime, 5, 1, 3, 1, 30, true,
                distanceGraph)){
            this.last=true;
            return false;
        }
        estadisticas.decreaseEstadisticas(ciudad.getDelete(),ciudad.getUpdate());
        ciudad.getDelete().clear();
        ciudad.getUpdate().clear();
        estadisticas.addEstadisticas(currentTime);
        currentTime = new Date(currentTime.getTime() + interval);
        return true;
    }


    private boolean optimizeCollapse() {
        clearUnused();
        distanceGraph = distanceGraphService.buildOrGetDistanceGraph(distanceGraph,currentTime,false);
        if(!ACOAlgorithm.optimizar(ciudad, currentTime, 5, 1, 3, 1, 50, true,
                distanceGraph)){
            this.last=true;
            return false;
        }
        estadisticas.decreaseEstadisticas(ciudad.getDelete(),ciudad.getUpdate());
        ciudad.getDelete().clear();
        ciudad.getUpdate().clear();
        estadisticas.addEstadisticas(currentTime);
        currentTime = new Date(currentTime.getTime() + interval);
        return true;
    }

    private void clearUnused() {
        ArrayList<Pedido> nuevosPedidos = ciudad.getPedidos().stream().filter(p->
                        p.getPorciones()==null || p.getPorciones().isEmpty() ||
                                p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||pp.getFechaEntrega().after(currentTime)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setPedidos(nuevosPedidos);
        ArrayList<Bloqueo>nuevosBloqueos= ciudad.getBloqueos().stream().filter(b->
                        !b.getFechaFin().before(currentTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setBloqueos(nuevosBloqueos);
    }

    public static void deleteUnused(Ciudad ciudad, Date currentTime) {
        ArrayList<Pedido> nuevosPedidos = ciudad.getPedidos().stream().filter(p->
                        p.getPorciones()==null || p.getPorciones().isEmpty() ||
                                p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||pp.getFechaEntrega().after(currentTime)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setPedidos(nuevosPedidos);
        ArrayList<Bloqueo>nuevosBloqueos= ciudad.getBloqueos().stream().filter(b->
                        !b.getFechaFin().before(currentTime))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ciudad.setBloqueos(nuevosBloqueos);
    }

    @JsonProperty("ciudad")
    public Ciudad getCiudad() {
        Ciudad ciudadClear = ciudad.getClearCity(currentTime);
        Date inicio = currentTime;
        List<Pedido> pedidos = ciudad.getPedidos().stream().filter(p-> (
                p.getFechaSolicitud().compareTo(inicio)<=0
                &&p.getPorciones().stream().anyMatch(pp->pp.getFechaEntrega()==null||pp.getFechaEntrega().compareTo(inicio)>=0)))
                .toList();
        ciudadClear.setPedidos(pedidos);
        return ciudadClear;
    }
}
