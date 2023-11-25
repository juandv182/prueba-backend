package fastglp.Algorithm.ACO;

import fastglp.model.Camino;
import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;

import java.util.Date;
import java.util.Objects;

public class Arista {
    public int node1;
    public int node2;
    public double pheromone;

    public Arista(int node1, int node2) {
        this.node1 = node1;
        this.node2 = node2;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Arista arista)) return false;
        return node1 == arista.node1 && node2 == arista.node2 ||
                node1 == arista.node2 && node2 == arista.node1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node1+node2, node2*node1);
    }
}