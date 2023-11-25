package fastglp.Algorithm.ACO;

import fastglp.model.*;
import fastglp.model.DistanceGraph;

import java.util.*;

public class Solution {
    public Graph graph;
    public double cost;
    public ArrayList<Arista> path;

    private final Date fechaInicio;
    private Ciudad ciudad;
    public Camion camion;
    public Date currentFecha;
    public Coordenada currentCoordenada;
    public int current;
    public double currentGLP;
    public double currentPetroleo;
    public double currentPeso;
    public ArrayList<PorcionPedido>pedidos;
    public boolean hasLost=false;
    private ArrayList<Pedido>pedidosCluster;
    private boolean valid=true;
    private DistanceGraph distanceGraph;

    public Solution(Graph graph, Date fechaInicio, Ciudad ciudad, Camion camion, DistanceGraph distanceGraph) {
        this.graph = graph;
        this.current= graph.indexOf(camion);
        this.path = new ArrayList<>();
        this.fechaInicio = fechaInicio;
        this.ciudad = ciudad;
        this.camion=camion;
        this.currentGLP=camion.getCurrentGLP();
        this.currentPetroleo=camion.getCurrentPetroleo();
        this.currentCoordenada=this.graph.getCoordenada(current);
        this.currentFecha = fechaInicio;
        this.currentPeso=camion.getPesoBase()+this.currentGLP*0.5;
        this.pedidosCluster=new ArrayList<>();
        this.distanceGraph=distanceGraph;
    }
    public Solution clearCopy(){
        return new Solution(graph,fechaInicio,ciudad,camion,distanceGraph);
    }

    public void prepareToBuildRoutes(){
        this.current= graph.indexOf(camion);
        this.currentFecha = fechaInicio;
        this.currentGLP=camion.getCurrentGLP();
        this.currentPetroleo=camion.getCurrentPetroleo();
        this.currentCoordenada=this.graph.getCoordenada(current);
        this.currentPeso=camion.getPesoBase()+this.currentGLP*0.5;
        this.pedidos=new ArrayList<>();
        assert pedidosCluster.stream().noneMatch(pc->pc.getPorciones().size()!=1);
        for (Pedido pedido : pedidosCluster) {
            pedidos.addAll(pedido.getPorciones());
        }
        this.pedidosCluster=null;
        this.graph=null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cost);
    }

    public ArrayList<Arista>iterator(){
        return path;
    }

    public int compareTo(Solution solution){
        return Double.compare(this.cost, solution.cost);
    }

    @Override
    public String toString() {
        ArrayList<Integer>camino=this.path.stream().map(a->a.node1).collect(ArrayList::new,ArrayList::add,ArrayList::addAll);
        camino.add(this.path.get(this.path.size()-1).node2);
        return "(Costo: "+cost + "\t\tCamino:"+camino+")";
    }
    public boolean addNode(Pedido pedido){
        return _addNode(pedido,graph.indexOf(pedido));
    }
    private boolean _addNode(Pedido pedido,int node) {
        Arista arista = this.graph.get(new Arista(this.current, node));
        this.path.add(arista);
        assert pedido.getGlp()<=this.camion.getCapacidadGLP();
        this.current = node;
        this.pedidosCluster.add(pedido);
        double dist = distanceGraph.getDistance( currentCoordenada,pedido.getCoordenada());
        double dist2 = distanceGraph.getDistance(ciudad.getAlmacenPrincipal().getCoordenada(),pedido.getCoordenada());
        double consumo = (dist * this.currentPeso + dist2 * (this.currentPeso-.5*pedido.getGlp())) / Camion.getConsumo();
        boolean needsRefill = this.currentGLP < pedido.getGlp() || consumo > this.currentPetroleo;
        if (needsRefill) {
            handleRefill(pedido);
        } else {
            updateCostAndDate(dist, false);
            updateResources(pedido, dist);
        }
        this.currentCoordenada = pedido.getCoordenada();
        if(currentFecha.getTime()<=pedido.getFechaLimite().getTime()-300000){
            return true;
        }else {
            this.valid=false;
            return false;
        }
    }

    private void handleRefill(Pedido pedido) {
        Almacen almacen = ciudad.getAlmacenPrincipal();
        double distToAlmacen = distanceGraph.getDistance(currentCoordenada,almacen.getCoordenada()) ;
        double dist = distanceGraph.getDistance(almacen.getCoordenada(),pedido.getCoordenada());
        this.currentGLP = camion.getCapacidadGLP();
        this.currentPetroleo = Camion.getCapacidadPetroleo();
        this.currentPeso = camion.getPesoBase() + this.currentGLP * 0.5;
        updateCostAndDate(dist + distToAlmacen, true);
        updateResources(pedido, dist);
    }

    private void updateCostAndDate(double distance, boolean isRefilling) {
        long time = (long) (distance / Camion.getVelocidad() * 3600000);
        if (isRefilling) {
            time += Camion.getTiempoDeCarga();
        }
        this.currentFecha = new Date(this.currentFecha.getTime() + time);
        this.cost += distance;
    }

    private void updateResources(Pedido pedido, double dist) {
        this.currentPetroleo -= dist * this.currentPeso / Camion.getConsumo();
        this.currentGLP -= pedido.getGlp();
        this.currentPeso = this.currentGLP * 0.5 + camion.getPesoBase();
    }

    public void opt2(int opt2) {
        if(pedidosCluster.size()<2)return;
        for (int iter = 0; iter < opt2; iter++) {
            int i = (int) (Math.random() * pedidosCluster.size());
            int j = (int) (Math.random() * pedidosCluster.size());
            if (i == j) {
                j = (j + 1) % pedidosCluster.size();
            }
            if (i > j) {
                int aux = i;
                i = j;
                j = aux;
            }
            swap2Opt(i, j);
        }
    }
    private void swap2Opt(int i,int j){
        Solution aux=this.clearCopy();
        ArrayList<Pedido>pedidos=new ArrayList<>(this.pedidosCluster);
        Collections.reverse(pedidos.subList(i,j+1));
        insertPedidos(aux, pedidos);
    }
    public void optOr(int optOr) {
        if(pedidosCluster.size()<2)return;
        int max= Math.min(pedidosCluster.size()-1,10);
        for (int iter = 0; iter < optOr; iter++) {
            int i = (int) (Math.random() * pedidosCluster.size());
            int j = i + (int) (Math.random() * Math.min(max,pedidosCluster.size()-i));
            int k = (int) (Math.random() * (pedidosCluster.size()+i-j));
            while ( k==i ){
                k=(k+1)%(pedidosCluster.size()+i-j);
            }
            swapOrOpt(i, j, k);
        }
    }

    private void swapOrOpt(int i, int j, int k) {
        Solution aux=this.clearCopy();
        ArrayList<Pedido>pedidos=new ArrayList<>(this.pedidosCluster);
        ArrayList<Pedido>auxPedidos=new ArrayList<>(pedidos.subList(i,j+1));
        pedidos.removeAll(auxPedidos);
        pedidos.addAll(k,auxPedidos);
        insertPedidos(aux, pedidos);
    }

    private void insertPedidos(Solution aux, ArrayList<Pedido> pedidos) {
        while(!pedidos.isEmpty()){
            Pedido pedido=pedidos.remove(0);
            if(!aux.addNode(pedido)||aux.cost>=this.cost){
                return;
            }
        }
        this.cost=aux.cost;
        this.path=aux.path;
        this.pedidosCluster=aux.pedidosCluster;
    }
}