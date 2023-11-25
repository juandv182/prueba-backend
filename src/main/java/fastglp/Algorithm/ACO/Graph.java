package fastglp.Algorithm.ACO;

import lombok.Getter;
import fastglp.model.Camion;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Graph {
    public HashMap<Arista, Arista> edges;
    public List<Camion> camiones;
    //son pedidoCluster
    @Getter
    private ArrayList<Pedido> pedidos;
    private HashMap<Integer, Coordenada> coordenadas;
    private Date fechaInicio;
    public int length;

    public Graph(List<Camion> camiones, Date fechaInicio, ArrayList<Pedido> pedidos) {
        //genera nodos para pedidos y luego camiones
        this.pedidos = pedidos;
        this.camiones = camiones;
        this.fechaInicio = fechaInicio;
        this.coordenadas = new HashMap<>(camiones.size()+pedidos.size());
        for(int i=0;i<pedidos.size();i++){
            this.coordenadas.put(i,pedidos.get(i).getCoordenada());
        }
        for(int i=0;i<camiones.size();i++){
            this.coordenadas.put(i+pedidos.size(),camiones.get(i).getUbicacion());
        }
        this.edges = new HashMap<>();
        this.length=pedidos.size()+camiones.size();
        for(int i=0;i<length;i++){
            int inicio=Math.max(0,i-1500);
            int fin=Math.min(length,i+2000);
            for(int j=inicio;j<fin;j++){
                if(i!=j){
                    Arista arista=new Arista(i,j);
                    arista.pheromone=1/this.getCoordenadaDifference(i,j);
                    this.edges.put(arista,arista);
                }else{
                    Arista arista=new Arista(i,j);
                    arista.pheromone=0;
                    this.edges.put(arista,arista);
                }
            }
        }
    }


    private long getTime(int nodo){
        if(isPedido(nodo)){
            return getPedido(nodo).getFechaLimite().getTime();
        } else if (isCamion(nodo)){
            return this.fechaInicio.getTime();
        } else {
            return 0;
        }
    }
    private double getTimeDifference(int nodo1, int nodo2){//en horas
        double diff=(double) Math.abs(getTime(nodo1) - getTime(nodo2)) /3600000;
        return diff<24?1*diff:(diff<48?1.5*diff:2*diff);
    }

    private double getCoordenadaDifference(int nodo1, int nodo2){
        return this.coordenadas.get(nodo1).distancia(this.coordenadas.get(nodo2))+1;
    }

    public Camion getCamion(int index) {
        return this.camiones.get(index - this.pedidos.size());
    }

    public Pedido getPedido(int index) {
        return this.pedidos.get(index);
    }

    public int indexOf(Camion camion) {
        return this.camiones.indexOf(camion) + this.pedidos.size();
    }
    public int indexOf(Pedido pedido) {
        return this.pedidos.indexOf(pedido);
    }

    public int getPedidosSize(){
        return this.pedidos.size();
    }
    public int getCamionesSize(){
        return this.camiones.size();
    }

    public Arista get(Arista arista){
        if(!this.edges.containsKey(arista)){
            arista.pheromone=1/(this.getCoordenadaDifference(arista.node1,arista.node2));
        }
        return this.edges.getOrDefault(arista,arista);
    }


    public Coordenada getCoordenada(int id) {
        return this.coordenadas.get(id);
    }
    public boolean isCamion(int id){
        return id>=this.pedidos.size();
    }
    public boolean isPedido(int id){
        return id<this.pedidos.size();
    }
    public ArrayList<Integer>indexesOfPedidos(){
        ArrayList<Integer> indexes=new ArrayList<>();
        for(int i=0;i<this.pedidos.size();i++){
            indexes.add(i);
        }
        return indexes;
    }
}
