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
@Table(name = "averia")
public class Averia {
    @Id
    @SequenceGenerator(name = "averia_sequence", sequenceName = "averia_sequence", initialValue = 1, allocationSize = 20)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "averia_sequence")
    @Column(name = "id")
    @Getter
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id", nullable = false)
    @JsonIgnore
    private Camion camion;
    private int tipo;// tipo 1 = llanta, tipo 2 = motor, tipo 3 = choque
    private Date fechaInicio;
    private Date fechaMovimiento;
    private Date fechaFin;


    public Averia(Camion camion, int tipo, Date fechaInicio) {
        this.camion = camion;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        calcularFechaFin();
    }
    private void calcularFechaFin(){
        //tipo 1 2 horas
        if(tipo==1){
            fechaFin = new Date(fechaInicio.getTime()+1000*60*60*2);
            fechaMovimiento = fechaFin;
        }else if(tipo==2){
            Calendar calendar = Calendar.getInstance();
            // generar un date a las 0 horas del dia de fechaInicio
            calendar.setTime(fechaInicio);
            calendar.set(Calendar.HOUR_OF_DAY,0);
            calendar.set(Calendar.MINUTE,0);
            calendar.set(Calendar.SECOND,0);
            for (int i = 0; i < 3; i++) {
                calendar.add(Calendar.HOUR_OF_DAY,8);
                Date fechaFinDia = calendar.getTime();
                if(fechaInicio.before(fechaFinDia)){
                    fechaFin = new Date(fechaFinDia.getTime()+1000*60*60*8);
                    fechaMovimiento = new Date(fechaInicio.getTime()+1000*60*60*2);
                    return;
                }
            }
        } else if (tipo == 3) {
            fechaMovimiento = new Date(fechaInicio.getTime()+1000*60*60*4);
            Calendar calendar = Calendar.getInstance();
            // generar un date a las 0 horas del dia de fechaInicio
            calendar.setTime(fechaInicio);
            calendar.set(Calendar.HOUR_OF_DAY,0);
            calendar.set(Calendar.MINUTE,0);
            calendar.set(Calendar.SECOND,0);
            calendar.add(Calendar.DAY_OF_MONTH,3);
            fechaFin = calendar.getTime();
        }
    }

    public boolean inRange(Date fecha){
        return fechaInicio.before(fecha) && fecha.before(fechaFin);
    }
}
