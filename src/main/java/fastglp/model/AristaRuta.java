package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "arista")
public class AristaRuta {
    @Id
    @SequenceGenerator(name = "arista_sequence", sequenceName = "arista_sequence", allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arista_sequence")
    @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    @JsonIgnore
    private Almacen almacen;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private PorcionPedido pedido;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "camino_id")
    private Camino camino;
    @JsonIgnore
    @Column(nullable = false)
    private double glpInicial;
    @JsonIgnore
    @Column(nullable = false)
    private double glpFinal;
    @JsonIgnore
    @Column(nullable = false)
    private double petroleoInicial;
    @JsonIgnore
    @Column(nullable = false)
    private double petroleoFinal;
    @JsonIgnore
    public String comment="";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id", nullable = false)
    @JsonIgnore
    private Camion camion;
    @JsonIgnore
    @Transient
    private Coordenada nextCoordenada;

    public AristaRuta(Camion camion,Object objeto, Camino camino,
                      double glpInicial,double glpFinal,double petroleoInicial,double petroleoFinal) {
        assert camino!=null;
        if(objeto instanceof Almacen){
            this.almacen = (Almacen) objeto;
        }else{
            this.pedido = (PorcionPedido) objeto;
        }
        this.camino = camino;
        this.glpInicial=glpInicial;
        this.glpFinal=glpFinal;
        this.petroleoInicial=petroleoInicial;
        this.petroleoFinal=petroleoFinal;
        this.camion=camion;
        assert !camino.getDestino().equals(camino.getOrigen()) || camino.getFechaFin().equals(camino.getFechaInicio()) :"el camino tiene fecha de inicio y fin diferentes";
    }

    @Override
    public String toString() {
        return  camino.getOrigen() +"->"+
                camino.getDestino() + "  |" + (almacen!=null?"almacen":"pedido");
    }

    @JsonProperty("tipo")
    public String getTipo(){
        return this.almacen!=null?"almacen":this.pedido!=null?"pedido":null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AristaRuta that)) return false;
        return Double.compare(getGlpInicial(), that.getGlpInicial()) == 0 && Double.compare(getGlpFinal(), that.getGlpFinal()) == 0 && Double.compare(getPetroleoInicial(), that.getPetroleoInicial()) == 0 && Double.compare(getPetroleoFinal(), that.getPetroleoFinal()) == 0 && Objects.equals(getId(), that.getId()) && Objects.equals(getAlmacen(), that.getAlmacen()) && Objects.equals(getPedido(), that.getPedido()) && Objects.equals(getCamino(), that.getCamino());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public Coordenada trunkInUbicacion(Date fecha) {
        this.pedido=null;
        this.almacen=null;
        this.camino=camino.trunkInUbicacion(fecha,this);
        this.camino.setFechaFin(fecha);
        return camino.getDestino();
    }
    private List<Coordenada>appendCoordenadas(List<Coordenada>prev,List<Coordenada>next,Coordenada nextCoordenada){
        //System.out.println("prev: "+prev);
        //System.out.println("next: "+next);
        Coordenada last=prev.get(prev.size()-1);
        List<Coordenada>coordenadas;
        if(!last.isInteger()){
            coordenadas=new ArrayList<>(prev.subList(0,nextCoordenada.equals(next.get(1))?prev.size()-1:prev.size()));
            coordenadas.addAll(next.subList(1,next.size()));
        }else{
            coordenadas=new ArrayList<>(prev.subList(0, prev.size()-1));
            coordenadas.addAll(next);
        }

        //System.out.println("result: "+coordenadas);
        return coordenadas;
    }


    public AristaRuta append(AristaRuta next) {
        assert next.getAlmacen()!=null || next.getPedido()!=null;
        AristaRuta newArista=new AristaRuta();
        List<Coordenada>coordenadas=appendCoordenadas(this.getCamino().getCoordenadas(),next.getCamino().getCoordenadas(),this.getNextCoordenada());
        Camino camino=new Camino(this.getCamino().getOrigen(),next.getCamino().getDestino(),coordenadas,this.getCamino().getFechaInicio());
        newArista.setCamino(camino);
        newArista.setGlpInicial(this.getGlpInicial());
        newArista.setGlpFinal(next.getGlpFinal());
        newArista.setPetroleoInicial(this.getPetroleoInicial());
        newArista.setPetroleoFinal(next.getPetroleoFinal());
        newArista.setAlmacen(next.getAlmacen());
        newArista.setPedido(next.getPedido());
        newArista.setCamion(this.getCamion());
        camino.setFechaFin(next.getCamino().getFechaFin());
        assert newArista.getAlmacen()!=null || newArista.getPedido()!=null;
        return newArista;
    }

    @JsonProperty("almacenId")
    public Long getAlmacenId(){
        return this.almacen!=null?this.almacen.getId():null;
    }

    public void clear() {
        this.nextCoordenada = null;
    }

    public AristaRuta copy() {
        AristaRuta aristaRuta=new AristaRuta();
        aristaRuta.setCamino(this.getCamino());
        aristaRuta.setGlpInicial(this.getGlpInicial());
        aristaRuta.setGlpFinal(this.getGlpFinal());
        aristaRuta.setPetroleoInicial(this.getPetroleoInicial());
        aristaRuta.setPetroleoFinal(this.getPetroleoFinal());
        aristaRuta.setAlmacen(this.getAlmacen());
        aristaRuta.setPedido(this.getPedido());
        aristaRuta.setCamion(this.getCamion());
        return aristaRuta;
    }
}
