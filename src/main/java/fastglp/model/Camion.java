package fastglp.model;


import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fastglp.Algorithm.ACO.Solution;
import fastglp.utils.UpdateOrDeleteListManager;
import fastglp.utils.Utils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "camion")
public class Camion {
    @Id
    @SequenceGenerator(name = "camion_sequence", sequenceName = "camion_sequence", initialValue = 1, allocationSize = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "camion_sequence")
    @Column(name = "id")
    @Setter
    private Long id;
    //capacidad total del camion
    @Column(name = "capacidadGLP", nullable = false)
    private double capacidadGLP;
    //capacidad actual del camion
    @Column(name = "currentGLP")
    @Setter
    private double currentGLP;
    //ubicacion actual del camion es la inicial si no esta en viaje
    @Setter
    @Column(name = "ubicacion")
    @Embedded
    private Coordenada ubicacion;
    //si esta en viaje, el camino que esta recorriendo

    @OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AristaRuta> ruta;
    @Column(name = "currentPetroleo")
    private double currentPetroleo;
    @Column(name = "pesoBase",nullable = false)
    private double pesoBase;

    @Getter
    private static double consumo=180;
    @Getter
    private static long tiempoDeCarga=0;
    @Getter
    private static double velocidad=50;
    @Getter
    private static double capacidadPetroleo =25;
    @JsonIgnore
    @Transient
    public Solution solution;
    @Column(name = "codigo",nullable = false)
    public String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    @JsonIgnore
    private Ciudad ciudad;

    public Camion(Camion camion){
        this.id=camion.id;
        this.capacidadGLP=camion.capacidadGLP;
        this.currentGLP=camion.currentGLP;
        this.ubicacion=camion.ubicacion;
        this.currentPetroleo=camion.currentPetroleo;
        this.pesoBase=camion.pesoBase;
        this.ruta=new ArrayList<>();
        this.ciudad=camion.ciudad;
        this.codigo=camion.codigo;
    }


    public Camion(double capacidadGLP, Coordenada ubicacion, double pesoBase) {
        this.capacidadGLP = capacidadGLP;
        this.currentGLP = capacidadGLP;
        this.ubicacion = ubicacion;
        this.currentPetroleo= capacidadPetroleo;
        this.pesoBase=pesoBase;
        this.ruta=new ArrayList<>();
        switch ((int) capacidadGLP){
            case 25:
                this.codigo="TA";
                break;
            case 15:
                this.codigo="TB";
                break;
            case 10:
                this.codigo="TC";
                break;
            case 5:
                this.codigo="TD";
        }
    }

    public Camion(double capacidadGLP, double currentGLP, Coordenada ubicacion, double currentPetroleo, double pesoBase) {
        this.capacidadGLP = capacidadGLP;
        this.currentGLP = currentGLP;
        this.ubicacion = ubicacion;
        this.currentPetroleo = currentPetroleo;
        this.pesoBase = pesoBase;
        this.ruta=new ArrayList<>();
        switch ((int) capacidadGLP){
            case 25:
                this.codigo="TA";
                break;
            case 15:
                this.codigo="TB";
                break;
            case 10:
                this.codigo="TC";
                break;
            case 5:
                this.codigo="TD";
        }
    }

    //conseguir la ubicacion del camion en un momento dado
    public Coordenada getUbicacion(Date fecha){
        //si no esta en viaje, la ubicacion es la ubicacion actual
        if(this.ruta.isEmpty()||this.ruta.get(0).getCamino().getFechaInicio().after(fecha)){
            return this.ubicacion;
        }
        Camino last=this.getCaminoFinal();
        if(last.getFechaFin().before(fecha)){
            return last.getDestino();
        }
        //calcular cual es el camino que esta recorriendo en el momento dado

        for (AristaRuta aristaRuta : this.ruta) {
            Camino camino = aristaRuta.getCamino();
            if (fecha.compareTo(camino.getFechaInicio()) >= 0 && fecha.compareTo(camino.getFechaFin()) <= 0) {
                //calcular la ubicacion del camion en el camino
                return camino.calcularUbicacion(fecha);
            }
        }
        //lanzar excepcion si no se encontro el camino
        throw new RuntimeException("No se encontro el camino, CAMION: "+this.id);
    }

    //conseguir la fecha y ubicacion del camion al finalizar todos los caminos
    @JsonIgnore
    public Camino getCaminoFinal(){
        if(this.ruta.isEmpty()){
            return null;
        }
        return this.ruta.get(this.ruta.size()-1).getCamino();
    }
    @JsonIgnore
    public AristaRuta getRutaFinal(){
        if(this.ruta.isEmpty()){
            return null;
        }
        return this.ruta.get(this.ruta.size()-1);
    }

    @JsonIgnore
    //conseguir la penultima coordenada
    public Coordenada getPenultimaCoordenada(){
        if(this.ruta.isEmpty()){
            return null;
        }
        List<Coordenada>coords=this.ruta.get(this.ruta.size()-1).getCamino().getCoordenadas();
        return coords.size()>=2? coords.get(coords.size()-2):null;
    }

    @Override
    public String toString() {
        return "Camion{" +
                "id=" + id +
                ", codigo='" + codigo + "'" +
                ", capacidadGLP=" + capacidadGLP +
                ", currentGLP=" + currentGLP +
                ", ubicacion=" + ubicacion +
                ", currentPetroleo=" + currentPetroleo +
                ", pesoBase=" + pesoBase +
                '}';
    }

    public void preparar(Date fecha, UpdateOrDeleteListManager delete,
                         UpdateOrDeleteListManager update){
        Map<Boolean, List<AristaRuta>> partitioned = ruta.stream()
                .collect(Collectors.partitioningBy(aristaRuta -> Utils.compareDate(aristaRuta.getCamino().getFechaInicio(), fecha) <= 0));
        this.ruta=new ArrayList<>(partitioned.get(true));
        delete.getAristasRuta().addAll(partitioned.get(false));
        this.ruta=ruta.stream().skip(Math.max(0, ruta.size() - 1)).collect(Collectors.toCollection(ArrayList::new));
        update.getAristasRuta().addAll(this.ruta.stream().map(AristaRuta::copy).toList());
        if(this.ruta.isEmpty()){
            return;
        }
        AristaRuta last=this.getRutaFinal();
        if(Utils.compareDate(last.getCamino().getFechaFin(),fecha)<=0){
            this.ubicacion= last.getCamino().getDestino();
            this.currentGLP=last.getGlpFinal();
            this.currentPetroleo=last.getPetroleoFinal();
            return;
        }
        //calcular cual es el camino que esta recorriendo en el momento dado
        Camino camino = last.getCamino();
        //if (fecha.compareTo(camino.getFechaInicio()) >= 0 && fecha.compareTo(camino.getFechaFin()) <= 0) {
            //calcular la ubicacion del camion en el camino
            if(Utils.compareDate(fecha,camino.getFechaFin())<0){
                Coordenada prevLast=last.getCamino().getDestino();
                this.ubicacion= last.trunkInUbicacion(fecha);
                assert this.ubicacion.equals(last.getCamino().getDestino());
                assert !this.ubicacion.equals(prevLast);
            }else {
                this.ubicacion= camino.calcularUbicacion(fecha);
            }
            this.currentGLP=last.getGlpInicial();
            double tiempo = (fecha.getTime() - camino.getFechaInicio().getTime()) / 3600000.0;
            double distanciaRecorrida = tiempo * Camion.getVelocidad();
            double peso=this.pesoBase+.5*this.currentGLP;
            this.currentPetroleo=last.getPetroleoInicial()-
                    distanciaRecorrida*peso/Camion.getConsumo();
        //}
        //System.out.println("No se encontro el camino, CAMION: "+this.id);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Camion camion)) return false;
        return Double.compare(getCapacidadGLP(), camion.getCapacidadGLP()) == 0 && Double.compare(getCurrentGLP(), camion.getCurrentGLP()) == 0 && Double.compare(getCurrentPetroleo(), camion.getCurrentPetroleo()) == 0 && Double.compare(getPesoBase(), camion.getPesoBase()) == 0 && Objects.equals(getId(), camion.getId()) && Objects.equals(getUbicacion(), camion.getUbicacion()) && Objects.equals(getRuta(), camion.getRuta());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public void addRuta(AristaRuta aristaRuta) {
        AristaRuta last=this.getRutaFinal();
        assert aristaRuta!=null&&(aristaRuta.getAlmacen()!=null||aristaRuta.getPedido()!=null);
        //assert last==null||Utils.compareDate(last.getCamino().getFechaFin(),aristaRuta.getCamino().getFechaInicio())<=0;
        if(last!=null&&last.getPedido()==null&&last.getAlmacen()==null){
            this.ruta.set(this.ruta.size()-1,last.append(aristaRuta));
            return;
        }
        this.getRuta().add(aristaRuta);
    }

    public void clear() {
        this.solution=null;
        this.ruta.forEach(AristaRuta::clear);
    }

}