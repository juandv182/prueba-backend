package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "porcion_pedido")
@Getter @Setter
@NoArgsConstructor
public class PorcionPedido {
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    @Id
    @SequenceGenerator(name = "pedido_porcion_sequence", sequenceName = "pedido_porcion_sequence", initialValue = 1, allocationSize = 4000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_porcion_sequence")
    @Column(name = "id")
    private Long id;
    private Date fechaEntrega;
    private double glp;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id")
    @JsonIgnore
    private Camion camion;
    @JsonIgnore
    private boolean asignado;
    private String cliente;
    private Date fechaLimite;
    @JsonIgnore
    @Transient
    private static long idGenerator = 1L;

    public PorcionPedido(Pedido p, double glp) {
        this.pedido=p;
        assert glp<=p.getNotAssignedGLP();
        this.glp=glp;
        this.id=idGenerator++;
        p.getPorciones().add(this);
        p.setNotAssignedGLP(p.getNotAssignedGLP()-glp);
        this.cliente=p.getCliente();
        this.fechaLimite=p.getFechaLimite();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PorcionPedido that)) return false;
        return Double.compare(getGlp(), that.getGlp()) == 0 &&
                isAsignado() == that.isAsignado() &&
                ((getPedido()==null&&that.getPedido()==null)||(getPedido()!=null&&that.getPedido()!=null&&
                        Objects.equals(getPedido().getId() ,that.getPedido().getId())))
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getFechaEntrega(), that.getFechaEntrega());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPedido(), getId());
    }

    @Override
    public String toString() {
        return "PorcionPedido{" +
                "pedido=" + pedido +
                ", id=" + id +
                ", fechaEntrega=" + fechaEntrega +
                ", glp=" + glp +
                ", camion=" + camion +
                ", asignado=" + asignado +
                ", cliente='" + cliente + '\'' +
                ", fechaLimite=" + fechaLimite +
                '}';
    }
}
