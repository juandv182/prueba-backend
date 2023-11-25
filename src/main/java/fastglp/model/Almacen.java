package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fastglp.utils.UpdateOrDeleteListManager;
import fastglp.utils.Utils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "almacen")
public class Almacen {
    @Id
    @SequenceGenerator(name = "almacen_sequence", sequenceName = "almacen_sequence", initialValue = 1, allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "almacen_sequence")
    @Column(name = "id")
    private Long id;
    @Getter
    @Embedded
    private Coordenada coordenada;

    @Column(nullable = false)
    private double capacidadTotal;

    @Setter
    private double capacidadActual;

    @Getter
    @Column(nullable = false)
    private boolean principal;

    @OneToMany(mappedBy = "almacen", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroAlmacen>registros;

    @Transient
    @JsonIgnore
    public List<Double> capacidadActualPorDia;
    @Transient
    @JsonIgnore
    public Date fechaInicioSimulacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    @JsonIgnore
    private Ciudad ciudad;
    public Almacen(Coordenada coordenada, double capacidadTotal, double capacidadActual, boolean principal) {
        this.coordenada = coordenada;
        this.capacidadTotal = capacidadTotal;
        this.capacidadActual = capacidadActual;
        this.principal = principal;
        if(this.principal){
            this.capacidadActual=1e200;
            this.capacidadTotal=1e200;
        }
        this.registros=new ArrayList<>();
    }

    //funcion copia
    public Almacen(Almacen almacen){
        this.id = almacen.id;
        this.coordenada = almacen.coordenada;
        this.capacidadTotal = almacen.capacidadTotal;
        this.capacidadActual = almacen.capacidadActual;
        this.principal = almacen.principal;
        this.registros=new ArrayList<>();
    }

    public double getCapacidadTotal() {
        return principal ? 1e200:capacidadTotal;
    }

    public double getCapacidadActual() {
        return principal ? 1e200:capacidadActual;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Almacen almacen)) return false;
        return Double.compare(getCapacidadTotal(), almacen.getCapacidadTotal()) == 0 && Double.compare(getCapacidadActual(), almacen.getCapacidadActual()) == 0 && isPrincipal() == almacen.isPrincipal() && Objects.equals(getId(), almacen.getId()) && Objects.equals(getCoordenada(), almacen.getCoordenada());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public void preparar(Date fechaInicio, UpdateOrDeleteListManager delete,
                         UpdateOrDeleteListManager update) {
        if(!this.isPrincipal()){
            Map<Boolean, List<RegistroAlmacen>> partitioned = registros.stream()
                    .collect(Collectors.partitioningBy(r -> Utils.compareDate(r.getFecha(), fechaInicio) <= 0));
            this.registros=partitioned.get(true).stream()
                    .skip(Math.max(0, partitioned.get(true).size() - 1))
                    .collect(Collectors.toCollection(ArrayList::new));
            this.capacidadActual=this.registros.stream()
                    .max(Comparator.comparing(RegistroAlmacen::getFecha))
                    .map(RegistroAlmacen::getGlp)
                    .orElse(this.capacidadActual);
            delete.getRegistrosAlmacen().addAll(partitioned.get(false));
            update.getRegistrosAlmacen().addAll(this.registros);
        }
    }

    @Override
    public String toString() {
        return "Almacen{" +
                "id=" + id +
                ", coordenada=" + coordenada +
                ", capacidadTotal=" + capacidadTotal +
                ", capacidadActual=" + capacidadActual +
                ", principal=" + principal +
                ", registros=" + registros +
                '}';
    }

    public void generarCalendarioGLP(Date fechaInicio,Date fechaFin){
        Calendar calendarFin=Calendar.getInstance();
        calendarFin.setTime(fechaFin);
        Calendar calendarInicio=Calendar.getInstance();
        calendarInicio.setTime(fechaInicio);
        this.capacidadActualPorDia = new ArrayList<>();
        this.fechaInicioSimulacion=fechaInicio;
        while(!calendarInicio.after(calendarFin)){
            this.capacidadActualPorDia.add(this.getCapacidadTotal());
            calendarInicio.add(Calendar.DAY_OF_MONTH,1);
        }
        this.capacidadActualPorDia.set(0,this.getCapacidadActual());
    }

    @JsonIgnore
    public double getGLP(Date fecha){
        if(this.isPrincipal())return 1e200;
        int index=(int) ((fecha.getTime()-this.fechaInicioSimulacion.getTime())/(24*3600*1000));
        if(index>=0 && index<this.capacidadActualPorDia.size()) {
            return this.capacidadActualPorDia.get(index);
        }
        System.out.println("se solicito GLP de un dia que no existe");
        return -1;
    }
    public double reducirGLP(Date fecha,double capacidad,boolean simulacion){
        if (this.isPrincipal()) return capacidad;
        int index = (int) ((fecha.getTime() - this.fechaInicioSimulacion.getTime()) / (24 * 3600 * 1000));
        if (index >= 0 && index < this.capacidadActualPorDia.size()) {
            double glp = this.capacidadActualPorDia.get(index);
            if (glp >= capacidad) {
                this.capacidadActualPorDia.set(index, glp - capacidad);
                if(!simulacion){
                    this.getRegistros().add(new RegistroAlmacen(this,fecha, glp - capacidad, 25));
                }
                return capacidad;
            } else {
                this.capacidadActualPorDia.set(index, 0.0);
                if(!simulacion){
                    this.getRegistros().add(new RegistroAlmacen(this,fecha, 0.0, 25));
                }

                return glp;
            }
        }
        System.out.println("se solicito GLP de un dia que no existe");
        return -1;
    }

    public void clear() {
        this.capacidadActualPorDia=null;
        this.fechaInicioSimulacion=null;
    }
}
