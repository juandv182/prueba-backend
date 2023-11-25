package fastglp.clustering;

import fastglp.model.*;

import fastglp.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ClusterAlgorithm {
    private static final double INCREMENT = 0.1;
    private static final long TIME_PROXIMITY=1000*60*10*0;
    private static final double DISTANCE_PROXIMITY=0;
    private static final Logger log= LoggerFactory.getLogger(ClusterAlgorithm.class);

    private static double getMaxCapacity(ArrayList<Integer>values,List<Camion>camiones, double[] pedidosPorCamion){
        double maxCapacity=-1;
        for (int i = values.size() - 1; i >= 0; i--) {
            if(pedidosPorCamion[i]-values.get(i)>=1){
                maxCapacity= camiones.get(i).getCapacidadGLP();
                break;
            }
        }
        //return Math.max(camiones.get(0).getCapacidadGLP(),maxCapacity);
        return 5;
    }

    private static int findIndexToReduce(ArrayList<Integer>values,double capacity,List<Double>capacities,List<Camion>camiones,
                                         double[] pedidosPorCamion){
        int index=-1;
        double diferenceMin=Double.MAX_VALUE;
        capacity = getCapacity(capacity, capacities);
        for (int i = values.size() - 1; i >= 0; i--) {
            if(Double.compare(camiones.get(i).getCapacidadGLP(), capacity)==0&&pedidosPorCamion[i]-values.get(i)<diferenceMin){
                index=i;
                diferenceMin=values.get(i)-pedidosPorCamion[i];
            }
        }
        return index;
    }

    private static double getCapacity(double capacity, List<Double> capacities) {
        for (int i = capacities.size() - 1; i >= 0; i--) {
            if(i==0) capacity = capacities.get(i);
            else if(capacity <= capacities.get(i)&& capacity > capacities.get(i-1)){
                capacity = capacities.get(i);
                break;
            }
        }
        return capacity;
    }

    private static int findIndexToAdd(ArrayList<Integer>values,double capacity,List<Double>capacities,List<Camion>camiones,
                                      double[] pedidosPorCamion){
        int index=-1;
        double diferenceMax=-Double.MAX_VALUE;//la diferencia maxima es el camion con menor capacidad que aun no ha sido llenado
        capacity = getCapacity(capacity, capacities);
        for (int i = values.size() - 1; i >= 0; i--) {
            if(Double.compare(camiones.get(i).getCapacidadGLP(), capacity)==0&&pedidosPorCamion[i]-values.get(i)>diferenceMax){
                index=i;
                diferenceMax=values.get(i)-pedidosPorCamion[i];
            }
        }
        return index;
    }

    private static double calcularBase(double valor,int potencia){
        return valor*(1-Math.pow(valor,potencia))/(1-valor);
    }

    private static double[]distribuirCantidad(double cantidad,int n){
        double[] arreglo = new double[n];
        double base=cantidad/calcularBase((1+INCREMENT),n);

        for (int i = arreglo.length - 1,j=1; i >= 0; i--,j++) {
            arreglo[i] = base * Math.pow(1 + INCREMENT, j);
        }

        return arreglo;
    }
    private static void prepararPedidos(List<Pedido>pedidos,Date fechaInicio,Ciudad ciudad){
        for(Pedido p:pedidos){
            for (int i = 0; i < p.getPorciones().size(); i++) {
                PorcionPedido porcionPedido=p.getPorciones().get(i);
                if(porcionPedido.getFechaEntrega()!=null&& Utils.compareDate( porcionPedido.getFechaEntrega(),fechaInicio)>0){
                    p.setNotAssignedGLP(p.getNotAssignedGLP()+porcionPedido.getGlp());
                    ciudad.getDelete().getPorcionPedidos().add(porcionPedido);
                    p.getPorciones().remove(i--);
                }
            }
        }
        pedidos.removeIf(p->Double.compare(p.getNotAssignedGLP(),0)==0);
    }
    public static ArrayList<PedidoCluster> buildClusters(List<Pedido>pedidos, List<Camion>camiones, Date fechaInicio,
                                             Ciudad ciudad) {
        //--ordenar pedidos y camiones por capacidad de GLP
        long startTime = System.currentTimeMillis();
        camiones.sort(Comparator.comparingDouble(Camion::getCapacidadGLP));
        pedidos= pedidos.stream().filter(p -> p.getFechaLimite().getTime()>=fechaInicio.getTime()
        &&p.getFechaSolicitud().getTime()<=fechaInicio.getTime()).collect(Collectors.toCollection(ArrayList::new));
        prepararPedidos(pedidos,fechaInicio,ciudad);
        //desordenar pedidos
        Collections.shuffle(pedidos);
        //Calcular cantidad de GLP que repartira cada camion, para que todos repartan cantidades similares a su capacidad
        double totalGLP = pedidos.stream().mapToDouble(Pedido::getNotAssignedGLP).sum();
        //cantidad de pedidos que repartira cada camion
        double cantidadIntermedia=totalGLP/camiones.stream().mapToDouble(Camion::getCapacidadGLP).sum();
        double[]pedidosPorCamion=distribuirCantidad(cantidadIntermedia*camiones.size(),camiones.size());
        //Inicializar un array con la cantidad actual de pedidos por camion
        ArrayList<Integer> pedidosPorCamionCurrent=new ArrayList<>(camiones.size());
        List<Double>capacities=camiones.stream().map(Camion::getCapacidadGLP).distinct().toList();
        for (int i = 0; i < camiones.size(); i++) {
            pedidosPorCamionCurrent.add(0);
        }

        //agrupar por coordenada y FechaEntrega
        ArrayList<PedidoCluster>clusters=new ArrayList<>();
        for (Pedido p : pedidos) {
            Coordenada c = p.getCoordenada();
            long fechaEntrega = p.getFechaLimite().getTime();
            double glp = p.getNotAssignedGLP();
            double maxCapacity = getMaxCapacity(pedidosPorCamionCurrent, camiones, pedidosPorCamion);
            for (PedidoCluster pc : clusters) {
                if (pc.getGlp() < maxCapacity
                        && pc.getCoordenadaCentral().distancia(c) <= DISTANCE_PROXIMITY
                        && Math.abs(pc.getFechaEntregaCentral() - fechaEntrega) <= TIME_PROXIMITY) {
                    double glpNeeded = Math.min(maxCapacity - pc.getGlp(), glp);
                    int indexToQuit = findIndexToReduce(pedidosPorCamionCurrent, pc.getGlp(), capacities, camiones, pedidosPorCamion);
                    pedidosPorCamionCurrent.set(indexToQuit, pedidosPorCamionCurrent.get(indexToQuit) - 1);
                    pc.addPorcion(new PorcionPedido(p, glpNeeded));
                    int indexToAdd = findIndexToAdd(pedidosPorCamionCurrent, pc.getGlp(), capacities, camiones, pedidosPorCamion);
                    pedidosPorCamionCurrent.set(indexToAdd, pedidosPorCamionCurrent.get(indexToAdd) + 1);
                    maxCapacity = getMaxCapacity(pedidosPorCamionCurrent, camiones, pedidosPorCamion);
                    glp -= glpNeeded;
                    if (Double.compare(glp, 0.0) <= 0) {
                        break;
                    }
                }
            }
            while (Double.compare(glp, 0.0) > 0) {
                double glpNeeded = Math.min(maxCapacity, glp);
                PedidoCluster pc = new PedidoCluster(new PorcionPedido(p, glpNeeded), ciudad, DISTANCE_PROXIMITY);
                glp -= glpNeeded;
                clusters.add(pc);
                int indexToAdd = findIndexToAdd(pedidosPorCamionCurrent, pc.getGlp(), capacities, camiones, pedidosPorCamion);
                pedidosPorCamionCurrent.set(indexToAdd, pedidosPorCamionCurrent.get(indexToAdd) + 1);
                maxCapacity = getMaxCapacity(pedidosPorCamionCurrent, camiones, pedidosPorCamion);
            }
        }
        long endTime = System.currentTimeMillis();
        log.debug(totalGLP+" GLP partido en "+clusters.size()+" clusters, en "+(endTime - startTime) + " ms");
        check(clusters,pedidos);
        return clusters;
    }
    private static void check(List<PedidoCluster> clusters,List<Pedido>pedidos){
        assert clusters.stream().noneMatch(pedidoCluster ->
             pedidoCluster.getGlp() != pedidoCluster.getPorciones().stream().mapToDouble(PorcionPedido::getGlp).sum()
        ) :"Error en la generacion de clusters";
        assert pedidos.stream().
                noneMatch(pedido ->
                        pedido.getNotAssignedGLP() != 0 || pedido.getGlp() != pedido.getPorciones().stream().mapToDouble(PorcionPedido::getGlp).sum())
        :"Error en la division de pedidos";
    }
}
