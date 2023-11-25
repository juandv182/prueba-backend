package fastglp.clustering;

import lombok.Getter;
import fastglp.model.*;


import java.util.ArrayList;
import java.util.Date;
@Getter
public class PedidoCluster extends Pedido {
    private final long fechaEntregaCentral;
    private final Coordenada coordenadaCentral;

    public PedidoCluster(PorcionPedido p, Ciudad ciudad,double distanciaProximidad) {
        super(p.getPedido().getId(), p.getPedido().getCoordenada(), p.getPedido().getFechaSolicitud(), p.getPedido().getPlazo(), p.getGlp(),
                p.getPedido().getCliente());
        this.setPorciones(new ArrayList<>());
        this.getPorciones().add(p);
        this.fechaEntregaCentral =p.getPedido().getFechaLimite().getTime();
        double newX=p.getPedido().getCoordenada().getX();
        double newY=p.getPedido().getCoordenada().getY();
        this.coordenadaCentral =new Coordenada(newX,newY);
    }

    public void addPorcion(PorcionPedido p){
        System.out.println("Agregando porcion "+p.getPedido().getId()+" al cluster "+this.getId());
        this.setGlp(this.getGlp()+p.getGlp());
        this.setFechaLimite(this.getFechaLimite().before(p.getPedido().getFechaLimite())?p.getPedido().getFechaLimite():this.getFechaLimite());
        this.setPlazo(((double) this.getFechaLimite().getTime()-this.getFechaSolicitud().getTime())/1000/60/60);
        this.getPorciones().add(p);
        this.setFechaLimite(new Date(this.getFechaLimite().getTime()-(long)(1.1*this.coordenadaCentral.distancia(p.getPedido().getCoordenada())/ Camion.getVelocidad()*3600000)));
    }
}
