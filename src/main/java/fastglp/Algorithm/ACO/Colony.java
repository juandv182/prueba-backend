package fastglp.Algorithm.ACO;

import fastglp.model.Ciudad;
import fastglp.model.DistanceGraph;

import java.util.ArrayList;
import java.util.Date;

public class Colony {
    public double alpha=1;
    public double beta=3;
    private Date fecha;
    private Ciudad ciudad;
    private DistanceGraph distanceGraph;

    public Colony(double alpha, double beta, Date fecha, Ciudad ciudad, DistanceGraph distanceGraph) {
        this.alpha = alpha;
        this.beta = beta;
        this.fecha = fecha;
        this.ciudad = ciudad;
        this.distanceGraph = distanceGraph;
    }

    public Colony() {
    }
    public ArrayList<Ant> getAnts(int size){
        ArrayList<Ant> ants = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ants.add(new Ant(this.alpha,this.beta,this.fecha,this.ciudad,this.distanceGraph));
        }
        return ants;
    }
}
