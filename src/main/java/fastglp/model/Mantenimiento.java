package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Calendar;
import java.util.Date;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mantenimiento")
public class Mantenimiento {
    @Id
    @SequenceGenerator(name = "mantenimiento_sequence", sequenceName = "mantenimiento_sequence", initialValue = 1, allocationSize = 20)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mantenimiento_sequence")
    @Column(name = "id")
    @Getter
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id", nullable = false)
    @JsonIgnore
    private Camion camion;
    private Date fechaInicio;
    private Date fechaFin;

    public Mantenimiento(Camion camion,int dia, int mes, int anio) {
        this.camion = camion;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(anio,mes,dia);
        this.fechaInicio = calendar.getTime();
        this.fechaFin = new Date(fechaInicio.getTime()+1000*60*60*24);
    }
    public boolean inRange(Date fecha){
        return fecha.after(fechaInicio) && fecha.before(fechaFin);
    }
}
