package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "camino")
public class Camino {
    @Id
    @SequenceGenerator(name = "camino_sequence", sequenceName = "camino_sequence", initialValue = 1, allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "camino_sequence")
    @Column(name = "id")
    private Long id;
    //distancia total del camino en km
    @JsonIgnore
    @Column( nullable = false)
    private double distancia;
    //tiempo total del camino en horas
    @JsonIgnore
    @Column( nullable = false)
    private double tiempo;
    //fecha de inicio y fin del camino
    @Column( nullable = false)
    private Date fechaInicio;
    @Column( nullable = false)
    private Date fechaFin;
    //coordenadas de origen y destino
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="x", column=@Column(name="origen_x", nullable = false)),
            @AttributeOverride(name="y", column=@Column(name="origen_y", nullable = false))
    })
    private Coordenada origen;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="x", column=@Column(name="destino_x", nullable = false)),
            @AttributeOverride(name="y", column=@Column(name="destino_y", nullable = false))
    })
    private Coordenada destino;
    //las coordenadas intermedias ordenadas de origen a destino (incluyendo origen y destino)
    @ElementCollection
    @CollectionTable(name = "camino_coordenadas", joinColumns = @JoinColumn(name = "camino_id"))
    private List<Coordenada> coordenadas;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Camino camino)) return false;
        return Objects.equals(getOrigen(), camino.getOrigen()) && Objects.equals(getDestino(), camino.getDestino());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrigen(), getDestino());
    }

    public Camino(Coordenada origen, Coordenada destino, List<Coordenada> coordenadas, Date fechaInicio) {
        this.origen = origen;
        this.destino = destino;
        this.coordenadas = coordenadas;
        //calcular la distancia total del camino
        assert origen.equals(coordenadas.get(0))&&destino.equals(coordenadas.get(coordenadas.size()-1)):"el origen y el destino no coinciden con las coordenadas "+
                "\norigen: "+origen+" destino: "+destino+"\ncoordenadas: "+coordenadas;
        this.distancia = 0.0;
        for (int i = 0; i < this.coordenadas.size() - 1; i++) {
            this.distancia += this.coordenadas.get(i).distancia(this.coordenadas.get(i + 1));
        }
        //calcular el tiempo total del camino
        this.tiempo = this.distancia / Camion.getVelocidad();
        this.fechaInicio = fechaInicio;
        //calcular fecha fin
        this.fechaFin = new Date(this.fechaInicio.getTime() + (long)(this.tiempo * 3600000));
    }

    //agregar nueva coordenada intermedia, actualizando el destino
    public void agregarCoordenada(Coordenada coordenada){
        this.coordenadas.add(coordenada);
        //agregar la distancia entre la ultima coordenada y la nueva
        this.distancia += this.coordenadas.get(this.coordenadas.size()-2).distancia(coordenada);
        //calcular el tiempo total del camino
        this.tiempo = this.distancia / Camion.getVelocidad();
        //actualizar el destino
        this.destino = coordenada;
        if(this.fechaInicio != null){
            this.fechaFin = new Date(this.fechaInicio.getTime() + (long)(this.tiempo * 3600000));
        }
    }
    //quitar ultima coordenada intermedia, actualizando el destino
    public void quitarUltimaCoordenada(){
        //quitar la ultima coordenada
        this.coordenadas.remove(this.coordenadas.size()-1);
        //quitar la distancia entre la ultima coordenada y la nueva
        this.distancia -= this.destino.distancia(this.coordenadas.get(this.coordenadas.size()-1));
        //calcular el tiempo total del camino
        this.tiempo = this.distancia / Camion.getVelocidad();
        //actualizar el destino
        this.destino = this.coordenadas.get(this.coordenadas.size()-1);
        //actualizar la fecha fin si existe fecha inicio
        if(this.fechaInicio != null){
            this.fechaFin = new Date(this.fechaInicio.getTime() + (long)(this.tiempo * 3600000));
        }
    }
    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
        //calcular fecha fin
        this.fechaFin = new Date(this.fechaInicio.getTime() + (long)(this.tiempo * 3600000));
    }

    //suponiendo movimiento de 50km/h, calcular la ubicacion del camion en un tiempo determinado
    public Coordenada calcularUbicacion(Date fecha){
        //si la fecha es mayor al tiempo  total del camino, el camion esta en el destino
        if(fecha.compareTo(this.fechaFin) >= 0){
            return this.destino;
        }
        //si la fecha es menor a la fecha de inicio del camino, el camion esta en el origen
        if(fecha.compareTo(this.fechaInicio) < 0){
            return null;
        }
        //si la fecha esta en el rango del camino, calcular la ubicacion
        //calcular la distancia recorrida en el tiempo dado
        //suponiendo movimiento de 50km/h
        //calculando el tiempo que estubo en el camino
        double tiempo = (fecha.getTime() - this.fechaInicio.getTime()) / 3600000.0;
        double distanciaRecorrida = tiempo * Camion.getVelocidad();

        double distanciaAcumulada = 0;
        for(int i=0;i<this.coordenadas.size()-1;i++){
            //si la distancia acumulada mas la distancia entre la coordenada actual y la siguiente es mayor a la distancia recorrida
            //la ubicacion del camion esta entre la coordenada actual y la siguiente
            if(distanciaAcumulada + this.coordenadas.get(i).distancia(this.coordenadas.get(i+1)) > distanciaRecorrida){
                //calcular la distancia entre la coordenada actual y la siguiente
                double distanciaEntreCoordenadas = this.coordenadas.get(i).distancia(this.coordenadas.get(i+1));
                //calcular la distancia recorrida entre la coordenada actual y la siguiente
                double distanciaRecorridaEntreCoordenadas = distanciaRecorrida - distanciaAcumulada;
                //calcular la proporcion de la distancia recorrida entre la coordenada actual y la siguiente
                double proporcion = distanciaRecorridaEntreCoordenadas / distanciaEntreCoordenadas;
                //calcular la ubicacion del camion
                double x = this.coordenadas.get(i).getX() + proporcion * (this.coordenadas.get(i+1).getX() - this.coordenadas.get(i).getX());
                double y = this.coordenadas.get(i).getY() + proporcion * (this.coordenadas.get(i+1).getY() - this.coordenadas.get(i).getY());
                return new Coordenada(x,y);
            }
            //si la distancia acumulada mas la distancia entre la coordenada actual y la siguiente es menor a la distancia recorrida
            //la distancia recorrida esta entre la coordenada actual y la siguiente
            else{
                //sumar la distancia entre la coordenada actual y la siguiente a la distancia acumulada
                distanciaAcumulada += this.coordenadas.get(i).distancia(this.coordenadas.get(i+1));
            }

        }
        //levantar excepcion si no se encontro la ubicacion
        throw new RuntimeException("No se encontro la ubicacion del camion");
    }
    public Camino trunkInUbicacion(Date fecha,AristaRuta aristaRuta) {

        //calcular la distancia recorrida en el tiempo dado
        //suponiendo movimiento de 50km/h
        //calculando el tiempo que estubo en el camino
        double distanciaRecorrida = (fecha.getTime() - this.fechaInicio.getTime()) * Camion.getVelocidad()/ 3600000.0;

        double distanciaAcumulada = 0;
        for(int i=0;i<this.coordenadas.size()-1;i++){
            //si la distancia acumulada mas la distancia entre la coordenada actual y la siguiente es mayor a la distancia recorrida
            //la ubicacion del camion esta entre la coordenada actual y la siguiente
            if( Double.compare(distanciaAcumulada + this.coordenadas.get(i).distancia(this.coordenadas.get(i+1)) , distanciaRecorrida)>0){
                //calcular la distancia entre la coordenada actual y la siguiente
                double distanciaEntreCoordenadas = this.coordenadas.get(i).distancia(this.coordenadas.get(i+1));
                //calcular la distancia recorrida entre la coordenada actual y la siguiente
                double distanciaRecorridaEntreCoordenadas = distanciaRecorrida - distanciaAcumulada;
                //calcular la proporcion de la distancia recorrida entre la coordenada actual y la siguiente
                double proporcion = distanciaRecorridaEntreCoordenadas / distanciaEntreCoordenadas;
                proporcion= Math.min(proporcion, 1.0);
                //calcular la ubicacion del camion
                Coordenada ubicacion=this.coordenadas.get(i).Add(this.coordenadas.get(i+1).Sub(this.coordenadas.get(i)).Mul(proporcion));
                //double x = this.coordenadas.get(i).getX() + proporcion * (this.coordenadas.get(i+1).getX() - this.coordenadas.get(i).getX());
                //double y = this.coordenadas.get(i).getY() + proporcion * (this.coordenadas.get(i+1).getY() - this.coordenadas.get(i).getY());
                //Coordenada ubicacion = new Coordenada(x,y);
                ArrayList<Coordenada>coordenadas1=new ArrayList<>(this.coordenadas.subList(0,i+1));
                coordenadas1.add(ubicacion);
                //assert !ubicacion.equals(this.coordenadas.get(i+1)):"la ubicacion no puede ser igual a la siguiente coordenada";
                aristaRuta.setNextCoordenada(this.coordenadas.get(i+1));
                return new Camino(this.origen,ubicacion,coordenadas1,this.fechaInicio);
            }
            //si la distancia acumulada mas la distancia entre la coordenada actual y la siguiente es menor a la distancia recorrida
            //la distancia recorrida esta entre la coordenada actual y la siguiente
            else{
                //sumar la distancia entre la coordenada actual y la siguiente a la distancia acumulada
                distanciaAcumulada += this.coordenadas.get(i).distancia(this.coordenadas.get(i+1));
            }

        }
        //levantar excepcion si no se encontro la ubicacion
        throw new RuntimeException("No se encontro la ubicacion del camion");
    }

    public void clear() {
        this.coordenadas.clear();
    }

    @Override
    public String toString() {
        return "Camino{" +
                "distancia=" + distancia +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", \ncoordenadas=" + coordenadas +
                '}';
    }
}
