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
@Table(name = "bloqueo")
public class Bloqueo {
    @Id
    @SequenceGenerator(name = "bloqueo_sequence", sequenceName = "bloqueo_sequence", initialValue = 1, allocationSize = 500)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bloqueo_sequence")
    @Column(name = "id")
    private Long id;

    @ElementCollection( fetch = FetchType.EAGER)
    @CollectionTable(name = "bloqueo_coordenadas", joinColumns = @JoinColumn(name = "bloqueo_id"))
    private List<Coordenada> coordenadas;

    @Setter
    @Column(nullable = false)
    private Date fechaInicio;

    @Setter
    @Column(nullable = false)
    private Date fechaFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    @JsonIgnore
    private Ciudad ciudad;

    public Bloqueo(Date fechaInicio, Date fechaFin) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.coordenadas=new ArrayList<>();
    }
    public void addCoordenada(Coordenada coordenada){
        if(coordenadas.isEmpty()||Double.compare(coordenadas.get(coordenadas.size()-1).distancia(coordenada),1)==0)
            coordenadas.add(coordenada);
        Coordenada lastCoord=coordenadas.get(coordenadas.size()-1);
        while(!lastCoord.equals(coordenada)){
            lastCoord=lastCoord.Add(coordenada.compare(lastCoord));
            coordenadas.add(lastCoord);
        }
    }
    public void addCoordenada(ArrayList<Coordenada> coordenada){
        coordenada.forEach(this::addCoordenada);
    }

    public int size(){
        return coordenadas.size()-1;
    }

    //algoritmo para saber si 2 segmentos se intersectan
    public boolean isLocked(Coordenada coordenada, Date fecha){
        //si el bloqueo no esta activo en la fecha
        if(fecha.after(fechaFin)||fecha.before(fechaInicio)){
            return false;
        }
        return this.coordenadas.stream().anyMatch(coord -> coord.equals(coordenada));
    }

    public boolean isLocked(Coordenada coordenada){
        return this.coordenadas.stream().anyMatch(coord -> coord.equals(coordenada));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bloqueo bloqueo)) return false;
        return Objects.equals(getId(), bloqueo.getId()) && Objects.equals(getCoordenadas(), bloqueo.getCoordenadas()) && Objects.equals(getFechaInicio(), bloqueo.getFechaInicio()) && Objects.equals(getFechaFin(), bloqueo.getFechaFin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
