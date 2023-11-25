package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "registro_almacen")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class RegistroAlmacen {

    @Id
    @SequenceGenerator(name = "registro_almacen_sequence", sequenceName = "registro_almacen_sequence", initialValue = 1, allocationSize = 500)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registro_almacen_sequence")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id", nullable = false)
    @JsonIgnore
    private Almacen almacen;

    @Column(nullable = false)
    private Date fecha;
    @Column(nullable = false)
    private double glp;
    @JsonIgnore
    @Column(nullable = false)
    private double petroleo;

    public RegistroAlmacen(Almacen almacen, Date fecha, double glp, double petroleo) {
        this.almacen = almacen;
        this.fecha = fecha;
        this.glp = glp;
        this.petroleo = petroleo;
    }

    @Override
    public String toString() {
        return "\nRegistroAlmacen{" +
                "fecha=" + fecha +
                ", glp=" + glp +
                ", petroleo=" + petroleo +
                '}';
    }
}
