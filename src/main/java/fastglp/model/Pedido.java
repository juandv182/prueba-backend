package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    @SequenceGenerator(name = "pedido_sequence", sequenceName = "pedido_sequence", initialValue = 1, allocationSize = 4000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_sequence")
    @Column(name = "id")
    private Long id;
    @OneToMany(mappedBy = "pedido",  fetch = FetchType.EAGER,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PorcionPedido> porciones;

    @Transient
    @JsonIgnore
    private double notAssignedGLP;
    @Setter
    @Column(name = "coordenada", nullable = false)
    @Embedded
    private Coordenada coordenada;
    @Column(name = "fechaSolicitud", nullable = false)
    private Date fechaSolicitud;
    @Column(name = "plazo", nullable = false)
    private double plazo;
    @Column(name = "fechaLimite", nullable = false)
    private Date fechaLimite;



    @Column(nullable = false)
    private double glp;

    private boolean entregado;

    private boolean cancelado;

    @Column(nullable = false)
    private String cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    @JsonIgnore
    private Ciudad ciudad;

    public Pedido(Pedido pedido) {
        this.id = pedido.id;
        this.coordenada = pedido.coordenada;
        this.fechaSolicitud = pedido.fechaSolicitud;
        this.plazo = pedido.plazo;
        this.glp = pedido.glp;
        this.cliente = pedido.cliente;
        this.fechaLimite = new Date(fechaSolicitud.getTime() + (long) (plazo * 60 * 60 * 1000));
        this.porciones = new ArrayList<>();
        this.notAssignedGLP = glp;
    }


    public List<PorcionPedido>getPorciones(Coordenada c){
        List<PorcionPedido>newPorciones=new ArrayList<>();
        while(!porciones.isEmpty()){
            PorcionPedido p=porciones.get(0);
            double distancia=c.distancia(p.getPedido().getCoordenada());
            int index=0;
            for (int i = 0; i < porciones.size(); i++) {
                if(c.distancia(porciones.get(i).getPedido().getCoordenada())<distancia){
                    p=porciones.get(i);
                    distancia=c.distancia(p.getPedido().getCoordenada());
                    index=i;
                }
            }
            newPorciones.add(p);
            porciones.remove(index);
        }
        porciones=newPorciones;
        return porciones;
    }

    public Pedido(Long id, Coordenada coordenada, Date fechaSolicitud, double plazo, double glp, String cliente) {
        this.id = id;
        this.coordenada = coordenada;
        this.fechaSolicitud = fechaSolicitud;
        this.plazo = plazo;
        this.glp = glp;
        this.cliente = cliente;
        //la fecha de entrega es la fecha solicitud mas el plazo en horas
        this.fechaLimite = new Date(fechaSolicitud.getTime() + (long) (plazo * 60 * 60 * 1000));
        this.porciones = new ArrayList<>();
        this.notAssignedGLP = glp;
    }

    public Pedido(Coordenada coordenada, Date fechaSolicitud, double plazo, double glp, String cliente) {
        this.coordenada = coordenada;
        this.fechaSolicitud = fechaSolicitud;
        this.plazo = plazo;
        this.glp = glp;
        this.cliente = cliente;
        //la fecha de entrega es la fecha solicitud mas el plazo en horas
        this.fechaLimite = new Date(fechaSolicitud.getTime() + (long) (plazo * 60 * 60 * 1000));
        this.porciones = new ArrayList<>();
        this.notAssignedGLP = glp;
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + id +
                ", coordenada=" + coordenada +
                ", fechaSolicitud=" + fechaSolicitud +
                ", plazo=" + plazo +
                ", fechaLimite=" + fechaLimite +
                ", glp=" + glp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedido pedido)) return false;
        return Double.compare(getPlazo(), pedido.getPlazo()) == 0 && Double.compare(getGlp(), pedido.getGlp()) == 0 && isEntregado() == pedido.isEntregado() && isCancelado() == pedido.isCancelado() && Objects.equals(getId(), pedido.getId()) && Objects.equals(getPorciones(), pedido.getPorciones()) && Objects.equals(getCoordenada(), pedido.getCoordenada()) && Objects.equals(getFechaSolicitud(), pedido.getFechaSolicitud()) && Objects.equals(getFechaLimite(), pedido.getFechaLimite()) && Objects.equals(getCliente(), pedido.getCliente());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
