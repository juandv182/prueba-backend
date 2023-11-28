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
    private List<Mantenimiento> mantenimientos;
    @OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Averia> averias;
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
    @JsonIgnore
    @Transient
    private Coordenada penultimaCoordenada;
    @JsonIgnore
    @Transient
    private Coordenada penultimaCoordenadaPrevIteracion;
    @JsonIgnore
    @Transient
    private Date fechaLibre;
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
        this.averias=new ArrayList<>();
        this.mantenimientos=new ArrayList<>();
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
        Date fechaCopy=fecha;
        this.fechaLibre=fecha;
        for (Mantenimiento mantenimiento : this.mantenimientos) {
            if (mantenimiento.inRange(fecha)) {
                fechaLibre = mantenimiento.getFechaFin();
                break;
            }
        }
        for (Averia averia : this.averias) {
            if (averia.inRange(fecha)) {
                fechaLibre = averia.getFechaFin();
                fecha=averia.getFechaInicio();
                break;
            }
        }
        Date finalFecha = fecha;
        Map<Boolean, List<AristaRuta>> partitioned = ruta.stream()
                .collect(Collectors.partitioningBy(aristaRuta -> Utils.compareDate(aristaRuta.getCamino().getFechaInicio(), finalFecha) <= 0));
        this.ruta=new ArrayList<>(partitioned.get(true));
        inicializarPrevCoordenada();
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
                this.penultimaCoordenada=prevLast;
                this.penultimaCoordenadaPrevIteracion=prevLast;
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
        for (Mantenimiento mantenimiento : this.mantenimientos) {
            if (mantenimiento.inRange(fechaCopy)) {
                doMaintenance(mantenimiento,fechaCopy);
                return;
            }
        }
        for (Averia averia : this.averias) {
            if (averia.inRange(fechaCopy)) {
                doRepair(averia,fechaCopy);
                return;
            }
        }
    }

    private void doRepair(Averia averia, Date fecha) {
        Camino camino;
        if(averia.getTipo()==1){
            camino=ciudad.AStar(this.ubicacion,this.ubicacion.floor(),fecha,
                    this.currentPetroleo,this.pesoBase/Camion.getConsumo(),
                    this.penultimaCoordenada);
            this.addRuta(new AristaRuta(this,averia,camino,this.currentGLP,
                    this.currentGLP,this.currentPetroleo,this.currentPetroleo));
        } else if (averia.getTipo()==2||averia.getTipo()==3){
            if(fecha.before(averia.getFechaMovimiento())){
                camino=ciudad.AStar(this.ubicacion,ciudad.getAlmacenPrincipal().getCoordenada(),
                        averia.getFechaMovimiento(),this.currentPetroleo,
                        this.pesoBase/Camion.getConsumo(),
                        this.penultimaCoordenada);
                this.addRuta(new AristaRuta(this,ciudad.getAlmacenPrincipal(),camino,
                        this.currentGLP,this.capacidadGLP,this.currentPetroleo,
                        Camion.getCapacidadPetroleo()));
                this.currentGLP=this.capacidadGLP;
                this.currentPetroleo=Camion.getCapacidadPetroleo();
            }else{
                camino=ciudad.AStar(this.ubicacion,ciudad.getAlmacenPrincipal().getCoordenada(),
                        fecha,this.currentPetroleo,this.pesoBase/Camion.getConsumo(),
                        this.penultimaCoordenada);
                this.addRuta(new AristaRuta(this,ciudad.getAlmacenPrincipal(),camino,
                        this.currentGLP,this.currentGLP,this.currentPetroleo,
                        Camion.getCapacidadPetroleo()));
                this.currentGLP=this.capacidadGLP;
                this.currentPetroleo=Camion.getCapacidadPetroleo();
            }
        }
    }

    public void doMaintenance(Mantenimiento mantenimiento,Date fecha){
        Camino camino =ciudad.AStar(this.ubicacion,ciudad.getAlmacenPrincipal().getCoordenada(),fecha,this.currentPetroleo,this.pesoBase/Camion.getConsumo(),
                this.penultimaCoordenada);
        this.addRuta(new AristaRuta(this, ciudad.getAlmacenPrincipal(),camino,this.currentGLP,this.getCapacidadGLP(),this.currentPetroleo,Camion.getCapacidadPetroleo()));
        this.currentGLP=this.getCapacidadGLP();
        this.currentPetroleo=Camion.getCapacidadPetroleo();
        this.ubicacion=ciudad.getAlmacenPrincipal().getCoordenada();
    }

    private void inicializarPrevCoordenada() {
        for (int i = this.ruta.size() - 1; i >= 0; i--) {
            Camino camino = this.ruta.get(i).getCamino();
            for (int j = camino.getCoordenadas().size() - 1; j >= 0; j--) {
                Coordenada c = camino.getCoordenadas().get(j);
                if (c.isInteger()&&!c.equals(this.penultimaCoordenadaPrevIteracion)) {
                    this.penultimaCoordenadaPrevIteracion = c;
                    // romper todos los loop
                    i = -1;
                    break;
                }
            }
        }
        this.penultimaCoordenada=this.penultimaCoordenadaPrevIteracion;
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
        actualizarPenultimaCoordenada(aristaRuta);
        AristaRuta last=this.getRutaFinal();
        assert aristaRuta!=null&&(aristaRuta.getAlmacen()!=null||aristaRuta.getPedido()!=null);
        //assert last==null||Utils.compareDate(last.getCamino().getFechaFin(),aristaRuta.getCamino().getFechaInicio())<=0;
        if(last!=null&&last.getPedido()==null&&last.getAlmacen()==null){
            this.ruta.set(this.ruta.size()-1,last.append(aristaRuta));
            return;
        }
        this.getRuta().add(aristaRuta);
    }

    private void actualizarPenultimaCoordenada(AristaRuta aristaRuta){
        List<Coordenada> coordenadas=aristaRuta.getCamino().getCoordenadas();
        if(coordenadas.size()==1){
            return;
        }
        for (int i = coordenadas.size() - 2; i >= 0; i--) {
            if(coordenadas.get(i).isInteger()){
                this.penultimaCoordenada=coordenadas.get(i);
                return;
            }
        }

    }

    public void clear() {
        this.solution=null;
        this.ruta.forEach(AristaRuta::clear);
    }

    public void addAveria(Averia averia) {
        this.averias.add(averia);
    }
}
