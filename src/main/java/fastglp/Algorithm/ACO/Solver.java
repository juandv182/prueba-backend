package fastglp.Algorithm.ACO;


import fastglp.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Solver {
    private static final double rho=0.03;
    private static final Logger log= LoggerFactory.getLogger(Solver.class);

    public Solver() {
    }

    // #1 escoje la mejor solucion generada por el metodo optimize(1.1)
    public ArrayList<Solution> solve(Graph graph, Colony colony, int genSize,
                                     int limit, int opt2, boolean multiThread){
        long startTime=System.currentTimeMillis();
        ArrayList<Solution> best=null;
        for(ArrayList<Solution> solutions : this.optimize(graph,colony,genSize,limit,opt2,multiThread)){
            if(best==null||calcularTotalCost(best)>calcularTotalCost(solutions)){
                best=solutions;
            }
        }
        if(best == null|| best.stream().anyMatch(s->s.hasLost)) {
            log.debug("No se encontro solucion");
            return null;
        }else {
            log.debug("Resuelto en "+(System.currentTimeMillis()-startTime)+" ms, con "+calcularTotalCost(best)+" metros");
            return best;
        }
    }

    private double calcularTotalCost(ArrayList<Solution> solutions){
        return solutions.stream().mapToDouble(s->s.cost).sum()* (solutions.get(0).hasLost?2:1);
    }

    public ArrayList<ArrayList<Solution>> optimize(Graph graph, Colony colony, int genSize,
                         int limit, int opt2, boolean multiThread){
        genSize=genSize>0?genSize:graph.length;
        ArrayList<Ant>ants=colony.getAnts(genSize);

        ArrayList<ArrayList<Solution>> result=new ArrayList<>();
        for(int i=0;i<limit;i++){
            ArrayList<ArrayList<Solution>> salesSolutions=(multiThread?this.findSolutionsMultithread(graph,ants,opt2,0):
                    this.findSolutions(graph,ants,opt2)).stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

            for(ArrayList<Solution> solutions:salesSolutions){
                solutions.sort(Solution::compareTo);
            }
            salesSolutions.sort((x,y)->{
                double sumX=calcularTotalCost(x);
                double sumY=calcularTotalCost(y);
                return Double.compare(sumX,sumY);
            });
            this.globalUpdate(salesSolutions,graph);
            if(!salesSolutions.isEmpty())result.add(salesSolutions.get(0));
        }
        if(result.isEmpty()){
            throw new RuntimeException("No se pudo encontrar una solucion, SOLVER");
        }
        return result;
    }
    public ArrayList<ArrayList<Solution>> findSolutionsMultithread(Graph graph, ArrayList<Ant> ants,
                                                        int opt2, int numThreads) {
        ArrayList<ArrayList<Solution>> salesSolutions = new ArrayList<>();
        // Si el número de hilos es 0, usa el número de procesadores disponibles - 1
        if (numThreads <= 0) {
            numThreads = Runtime.getRuntime().availableProcessors() - 1;
        }
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<ArrayList<Solution>>> futures = new ArrayList<>();
        for (Ant ant : ants) {
            Future<ArrayList<Solution>> future = executor.submit(() -> ant.tour(graph, opt2));
            futures.add(future);
        }
        for (Future<ArrayList<Solution>> future : futures) {
            try {
                salesSolutions.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return salesSolutions;
    }
    private ArrayList<ArrayList<Solution>> findSolutions(Graph graph,ArrayList<Ant>ants,
                                                         int opt2){
        ArrayList<ArrayList<Solution>> salesSolutions=new ArrayList<>();
        for (Ant ant : ants) {
            salesSolutions.add(ant.tour(graph, opt2));
        }
        return salesSolutions;
    }


    private void globalUpdate(ArrayList<ArrayList<Solution>> salesSolutions,Graph graph){
        HashMap<Arista,Double>nextPheromones=new HashMap<>();
        for(ArrayList<Solution> solutions:salesSolutions){
            double cost=calcularTotalCost(solutions);
            for(Solution solution:solutions){
                for(Arista arista:solution.iterator()){
                    nextPheromones.put(arista, nextPheromones.getOrDefault(arista, 0.0) + 1/cost);
                }
            }
        }
        for(Arista arista :graph.edges.values()){
            arista.pheromone=(1.0- rho)* arista.pheromone+nextPheromones.getOrDefault(arista,0.0);
        }
    }
}