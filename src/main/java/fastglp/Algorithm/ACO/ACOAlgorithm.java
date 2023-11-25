package fastglp.Algorithm.ACO;

import fastglp.clustering.ClusterAlgorithm;
import fastglp.clustering.PedidoCluster;
import fastglp.model.Almacen;
import fastglp.model.Camion;
import fastglp.model.Ciudad;
import fastglp.model.Pedido;
import fastglp.model.DistanceGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class ACOAlgorithm {
    private static final Logger log= LoggerFactory.getLogger(ACOAlgorithm.class);
    public static boolean optimizar(Ciudad ciudad, Date fechaInicio, int ants, double alpha, double betha,
                                    int limit, int opt2, boolean multiThread, DistanceGraph distanceGraph){
        //el id del camion sera su id+sizeof(Pedidos)
        log.debug("##########################################################");
        log.debug("ACO Algorithm iniciado");

        Solver solver=new Solver();
        Colony colony=new Colony(alpha,betha,fechaInicio,ciudad,distanceGraph);
        ArrayList<PedidoCluster>pc=ClusterAlgorithm.buildClusters(ciudad.getPedidos(),ciudad.getCamiones(),fechaInicio,ciudad);
        Graph graph = createGraph(ciudad,fechaInicio, pc);
        ArrayList<Solution>best;
        try {
            best=solver.solve(
                    graph,
                    colony,
                    ants,limit,opt2,multiThread);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        long start=System.currentTimeMillis();
        if(best==null){
            log.debug("No se encontro solucion");
            return false;
        }
        try {
            ciudad.buildRutas(best,fechaInicio,distanceGraph);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        ciudad.getCamiones().forEach(Camion::clear);
        ciudad.getAlmacenes().forEach(Almacen::clear);
        log.debug("Rutas asignadas en "+(System.currentTimeMillis()-start)+" ms, con "+ciudad.getCamiones().stream()
                .flatMapToDouble(c -> c.getRuta().stream().mapToDouble(a -> a.getCamino().getDistancia()))
                .sum()+" metros, con "+ciudad.getCamiones().stream()
                .flatMapToDouble(c -> c.getRuta().stream()
                        .mapToDouble(a -> "pedido".equals(a.getTipo()) ? a.getPedido().getGlp() : 0.0))
                .sum()+" GLP");
        return true;
    }
    private static Graph createGraph(Ciudad ciudad,Date fechaInicio,ArrayList<PedidoCluster>pc){
        //actualizar la posicion de los camiones
        //agregar 4 horas a la fecha de inicio
        ciudad.getCamiones().forEach(c->{
            c.preparar(fechaInicio,ciudad.getDelete(),ciudad.getUpdate());
        });
        ciudad.getAlmacenes().forEach(a->{
            a.preparar(fechaInicio,ciudad.getDelete(),ciudad.getUpdate());
        });
        //grap recibe un arrraylist de camiones y un arraylist de pedidos
        ciudad.getCamiones().sort(Comparator.comparingDouble(Camion::getCapacidadGLP));
        //pedidos ordenados por fecha limite
        long start=System.currentTimeMillis();
        Graph graph = new Graph(
                ciudad.getCamiones(),fechaInicio,
                pc.stream()
                        .sorted(Comparator.comparing(Pedido::getFechaLimite))
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
        );
        log.debug("Grafo generado en "+(System.currentTimeMillis()-start)+" ms");
        return graph;
    }
}