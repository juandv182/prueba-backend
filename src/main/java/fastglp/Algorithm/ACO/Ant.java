package fastglp.Algorithm.ACO;

import fastglp.model.*;
import fastglp.model.DistanceGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Ant {
    public double alpha;
    public double beta;
    public int vendedores;
    public Graph graph;
    public int n;
    private Date fecha;
    private Ciudad ciudad;
    private static final Logger log= LoggerFactory.getLogger(Ant.class);
    private DistanceGraph distanceGraph;

    public Ant(double alpha, double beta, Date fecha, Ciudad ciudad, DistanceGraph distanceGraph) {
        this.alpha = alpha;
        this.beta = beta;
        this.vendedores = -1;
        this.n = -1;
        this.fecha = fecha;
        this.ciudad = ciudad;
        this.distanceGraph=distanceGraph;
    }
    // #1.1.1
    // se crea una lista de soluciones
    public ArrayList<Solution>tour(Graph graph, int opt2) {
        this.graph=graph;
        this.vendedores =graph.getCamionesSize();
        this.n=graph.length;
        ArrayList<Solution> solutions=new ArrayList<>();

        for(int i = 0; i< this.vendedores; i++){
            solutions.add(new Solution(graph,this.fecha,this.ciudad,graph.camiones.get(i),distanceGraph));
        }
        ArrayList<Integer>unvisited= IntStream.range(0, n-vendedores).boxed().collect(Collectors.toCollection(ArrayList::new));
        long fechaMinima;
        //se crea un arreglo con la iteracion actual del vendedor, se inicia en cero
        while(!unvisited.isEmpty()){
            ArrayList<Solution>candidatos=new ArrayList<>(solutions);
            int index=unvisited.remove(0);
            fechaMinima=candidatos.stream().map(s->s.currentFecha).min(Date::compareTo).orElse(this.fecha).getTime();
            //Solution mejorCandidato=this.chooseCandidate(candidatos,index,fechaMinima);
            Solution mejorCandidato=this.chooseBest(candidatos,index,fechaMinima);
            if(mejorCandidato!=null) {
                boolean res = mejorCandidato.addNode(graph.getPedido(index));
                assert res;
            }else{
                unvisited.stream().map(graph::getPedido).forEach(System.out::println);
                //throw new RuntimeException("No se pudo encontrar una solucion, ANT");
                return null;
            }
        }
        if(opt2>=1){
            for(Solution solution:solutions){
                solution.optOr(opt2);
            }
        }
        return solutions;
    }

    private Solution chooseBest(ArrayList<Solution> candidatos, int index, long fechaMinima) {
        Solution best=null;
        double bestScore=0.0;
        for(Solution candidate:candidatos){
            double score=scoreEdge(candidate,index,fechaMinima);
            if(Double.compare(score,bestScore)>0){
                best=candidate;
                bestScore=score;
            }
        }
        return best;
    }


    private Solution chooseCandidate(ArrayList<Solution>candidatos,int pedido,long fechaMinima){
        // se calcula el score de cada nodo
        List<Double>scores=getScores(candidatos,pedido,fechaMinima);
        if(candidatos.size()==1){
            return candidatos.get(0);
        } else if (candidatos.isEmpty()) {
            return null;
        }
        // se escoje un nodo aleatorio la prob es proporcional a su score
        return peekOneCandidate(candidatos,scores);
    }
    private Solution peekOneCandidate(ArrayList<Solution> candidatos, List<Double> scores) {
        double total = 0.0;
        double[] cumDist = new double[scores.size()];
        for (int i = 0; i < scores.size(); i++) {
            total += scores.get(i);
            cumDist[i] = total;
        }
        double randomValue = new Random().nextDouble() * total;
        for (int i = 0; i < cumDist.length; i++) {
            if (randomValue <= cumDist[i]) {
                return candidatos.get(i);
            }
        }
        return candidatos.get(candidatos.size() - 1);
    }

    private List<Double>getScores(List<Solution>candidates,int pedido, long fechaMinima){
        List<Double>scores=new ArrayList<>(candidates.size());
        List<Solution>candidates1=new ArrayList<>(candidates);
        for(Solution candidate:candidates1){
            double score=scoreEdge(candidate,pedido,fechaMinima);
            if(Double.compare(score,0.0)>0){
                scores.add(score);
            }else{
                candidates.remove(candidate);
            }
        }
        return scores;
    }
    private double scoreEdge(Solution candidate,int pedido,long fechaMinima){
        double weight= evaluateOption(candidate,pedido, fechaMinima);
        Arista arista=this.graph.get(new Arista(candidate.current,pedido));
        double phe = arista.pheromone;
        return Double.compare(weight,0)>=0?Math.pow(phe, alpha) * Math.pow(1.0 / weight, beta):-1;
    }

    public Almacen getAlmacenMasCercano(Coordenada origen,Coordenada destino,double glp){
        //se debuelbe el almacen principal
        return ciudad.getAlmacenPrincipal();
    }

    private double evaluateOption(Solution camino,int nexNode, long fechaMinima) {
        //auxiliares-----------------------------------------------
        Pedido pedido = graph.getPedido(nexNode);
        Camion camion = camino.camion;
        Coordenada current=camino.currentCoordenada;
        Coordenada destino=pedido.getCoordenada();
        if(pedido.getGlp()>camion.getCapacidadGLP())return -1;//no es posible entregar este pedido
        if(camino.currentFecha.after(pedido.getFechaLimite()))return -1;//no es posible entregar este pedido
        double currentGLP=camino.currentGLP;
        double currentPetroleo=camino.currentPetroleo;
        double currentPeso=camino.currentPeso;
        double dist1= distanceGraph.getDistance(current,destino);
        double tiempoDeCarga=0.0;
        double s1;
        Almacen almPedido=ciudad.getAlmacenPrincipal();
        double totalDistance=dist1;
        //---------------------------------------------------------
        if(currentGLP<pedido.getGlp()||(dist1*currentPeso/Camion.getConsumo()+distanceGraph.getDistance(almPedido.getCoordenada(),destino)
                *(currentPeso-.5*pedido.getGlp())/Camion.getConsumo())>currentPetroleo) {
            // en el almacen
            //Almacen almacenCercano=getAlmacenMasCercano(current,destino,currentGLP>=pedido.getGlp()?0.0:pedido.getGlp()-currentGLP);
            Almacen almacenCercano=almPedido;
            double dist2=distanceGraph.getDistance(almacenCercano.getCoordenada(),pedido.getCoordenada());
            dist1=distanceGraph.getDistance(current,almacenCercano.getCoordenada());
            totalDistance=dist2+dist1;
            tiempoDeCarga=Camion.getTiempoDeCarga();
        }
        //con 5 minutos de olgura
        //s1=totalDistance/Camion.getVelocidad()+tiempoDeCarga/3600000.0+5/60.0;
        //double plazo=(pedido.getFechaLimite().getTime()-camino.currentFecha.getTime())/3600000.0;
        long llegada=(long)(totalDistance*3600000/Camion.getVelocidad())+camino.currentFecha.getTime();
        if(llegada>pedido.getFechaLimite().getTime()-300000) return -1;
        //return totalDistance+1/(plazo-s1);
        return (llegada-fechaMinima+1)/360000.0;
    }
}
