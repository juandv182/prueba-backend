package fastglp.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fastglp.Algorithm.ACO.Solution;
import fastglp.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import static java.util.stream.Collectors.toMap;

@Getter @Setter
public class Estadisticas {
    private Date fechaInicio;
    private Date fechaFin;
    private long tiempoTotal;
    private int totalPorcionPedidos;
    private int totalReplanificaciones;
    private double totalKmRecorridos;
    private double totalGLP;
    private double totalPetroleo;
    @JsonIgnore
    private double totalTiempoDeEntrega;
    private Map<String,EstadisticasPorCamion> estadisticasPorCamion;
    private Map<Long,Integer> numRecargasPorAlmacen;
    @JsonIgnore
    private List<List<Solution>> ultimaPlanificacion;
    @JsonIgnore
    private Ciudad ciudad;
    @JsonIgnore
    private final Set <Long> pedidos=new HashSet<>();
    
    @JsonProperty("tiempoPromedioDeEntrega")
    public double getTiempoPromedioDeEntrega(){
        return totalTiempoDeEntrega/totalPorcionPedidos/1000/60;
    }
    
    public Estadisticas(Ciudad ciudad,Date fechaInicio){
        this.ciudad=ciudad;
        this.fechaInicio=fechaInicio;
        clear();
    }

    public void addEstadisticas(Date fechaFin){
        this.fechaFin=fechaFin;
        this.tiempoTotal=fechaFin.getTime()-fechaInicio.getTime();
        totalReplanificaciones ++;
        for (Camion c : ciudad.getCamiones()) {
            EstadisticasPorCamion e=estadisticasPorCamion.get(c.getCodigo());
            for (AristaRuta r : c.getRuta()) {
                totalKmRecorridos += r.getCamino().getDistancia();
                e.camionTotalKmRecorridos+=r.getCamino().getDistancia();
                totalPetroleo += Math.max(r.getPetroleoInicial()-r.getPetroleoFinal(),0);
                e.camionTotalPetroleo+=Math.max(r.getPetroleoInicial()-r.getPetroleoFinal(),0);
                if(Objects.equals(r.getTipo(), "pedido")){
                    totalTiempoDeEntrega += r.getPedido().getFechaLimite().getTime()-r.getCamino().getFechaFin().getTime();
                    totalGLP += r.getPedido().getGlp();
                    e.camionTotalGLP+=r.getPedido().getGlp();
                    e.camionTotalPorcionPedidos++;
                    totalPorcionPedidos++;
                    pedidos.add(r.getPedido().getId());
                }else {
                    numRecargasPorAlmacen.put(r.getAlmacen().getId(),
                            numRecargasPorAlmacen.get(r.getAlmacen().getId()) + 1);
                }
            }
        }
    }

    public void decreaseEstadisticas(UpdateOrDeleteListManager delete, UpdateOrDeleteListManager update){
        decrease(delete);
        decrease(update);
    }

    private void decrease(UpdateOrDeleteListManager changed){
        for (AristaRuta ar : changed.getAristasRuta()) {
            EstadisticasPorCamion e = estadisticasPorCamion.get(ar.getCamion().getCodigo());
            totalKmRecorridos -= ar.getCamino().getDistancia();
            e.camionTotalKmRecorridos -= ar.getCamino().getDistancia();
            totalPetroleo -= Math.max(ar.getPetroleoInicial() - ar.getPetroleoFinal(), 0);
            e.camionTotalPetroleo -= Math.max(ar.getPetroleoInicial() - ar.getPetroleoFinal(), 0);
            if (Objects.equals(ar.getTipo(), "pedido")) {
                totalTiempoDeEntrega -= ar.getPedido().getFechaLimite().getTime() - ar.getCamino().getFechaFin().getTime();
                totalGLP -= ar.getPedido().getGlp();
                e.camionTotalGLP -= ar.getPedido().getGlp();
                e.camionTotalPorcionPedidos--;
                totalPorcionPedidos--;
            }else {
                numRecargasPorAlmacen.put(ar.getAlmacen().getId(),
                        numRecargasPorAlmacen.get(ar.getAlmacen().getId()) - 1);
            }
        }
    }

    public void clear(){
        this.tiempoTotal=0;
        this.totalPorcionPedidos=0;
        this.totalReplanificaciones=0;
        this.totalKmRecorridos=0;
        this.totalGLP=0;
        this.totalPetroleo=0;
        this.totalTiempoDeEntrega=0;
        this.estadisticasPorCamion=ciudad.getCamiones().stream().collect(
                toMap(Camion::getCodigo, EstadisticasPorCamion::new));
        this.numRecargasPorAlmacen=ciudad.getAlmacenes().stream().collect(
                toMap(Almacen::getId,a->(Integer) 0));
        this.ultimaPlanificacion=null;
        this.pedidos.clear();
    }

    @Getter @Setter
    public static class EstadisticasPorCamion{
        public int  camionTotalPorcionPedidos;
        public double camionTotalGLP;
        public double camionTotalKmRecorridos;
        public double camionTotalPetroleo;
        @JsonIgnore
        private final Camion camion;

        public EstadisticasPorCamion(Camion camion) {
            this.camion = camion;
        }

        @JsonProperty("camionId")
        public Long getCamionId(){
            return camion.getId();
        }
    }
    @JsonProperty("totalPedidos")
    public int getTotalPedidos(){
        return pedidos.size();
    }
}
