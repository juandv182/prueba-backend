package fastglp.model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.utils.KeyPair;
import fastglp.utils.Utils;
import fastglp.utils.json.KeyPairDeserializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "distance_graph")
public class DistanceGraph {
    @Id
    @SequenceGenerator(name = "distance_graph_sequence", sequenceName = "distance_graph_sequence", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "distance_graph_sequence")
    @Column(name = "id")
    private Long id;
    @Lob
    @Column(name = "distances_compressed", columnDefinition="LONGBLOB")
    private byte[] distancesCompressed;
    @Transient
    private Map<KeyPair<Coordenada>,Double>distanceMap=new ConcurrentHashMap<>();
    private int maxX;
    private int maxY;
    @Column(name = "time_interval")
    private Long interval;
    private Date build;
    @Transient
    private final Object lock=new Object();
    @Transient
    private Ciudad ciudad;
    public DistanceGraph(Ciudad ciudad, double horasEach) {
        this.maxX=ciudad.getMaxX();
        this.maxY=ciudad.getMaxY();
        this.ciudad=ciudad;
        this.interval=(long)(horasEach*60*60*1000);
    }
    public DistanceGraph(Ciudad ciudad, long interval) {
        this.maxX=ciudad.getMaxX();
        this.maxY=ciudad.getMaxY();
        this.ciudad=ciudad;
        this.interval=interval;
    }

    public boolean valid(Date init){
        return build !=null&&init.getTime()- build.getTime()<=interval;
    }


    public void buildGraph(Date init) {
        Date end = new Date(init.getTime() + interval);
        long start = System.currentTimeMillis();
        HashSet<Coordenada> bloqueos = ciudad.getBloqueos().stream()
                .filter(bloqueo -> !init.after(bloqueo.getFechaFin()) && !bloqueo.getFechaInicio().after(end))
                .flatMap(b -> b.getCoordenadas().stream())
                .collect(Collectors.toCollection(HashSet::new));
        List<Coordenada> allcoords = getAllCoordenadas();
        ForkJoinPool forkJoinPool = new ForkJoinPool(5); // Crear un ForkJoinPool con 5 hilos
        try {
            forkJoinPool.submit(() ->
                    IntStream.range(0, allcoords.size()).parallel().forEach(i -> {
                        for (int j = i + 1; j < allcoords.size(); j++) {
                            Coordenada origen = allcoords.get(i);
                            Coordenada destino = allcoords.get(j);
                            Camino camino = AStar(origen, destino, bloqueos);
                            if (camino != null) {
                                synchronized(lock) {
                                    distanceMap.put(new KeyPair<>(origen, destino), camino.getDistancia());
                                }
                            } else {
                                System.out.println("No se pudo encontrar camino entre " + origen + " y " + destino);
                            }
                        }
                    })
            ).get(); // Espera a que se complete la tarea
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        //IntStream.range(0, allcoords.size()).parallel().forEach(i -> {
        //    for (int j = i + 1; j < allcoords.size(); j++) {
        //        Coordenada origen = allcoords.get(i);
        //        Coordenada destino = allcoords.get(j);
        //        Camino camino = AStar(origen, destino, bloqueos);
        //        if (camino != null) {
        //            synchronized(lock) {
        //                distanceMap.put(new KeyPair<>(origen, destino), camino.getDistancia());
        //            }
        //        }else {
        //            System.out.println("No se pudo encontrar camino entre " + origen + " y " + destino);
        //        }
        //    }
        //});
        this.build=init;
        System.out.println("Se ha construido el grafo de distancias en " + (System.currentTimeMillis() - start) + " ms");
    }

    public double getDistance(Coordenada c1, Coordenada c2){
        if(!c1.isInteger())c1=c1.floor();
        return distanceMap.getOrDefault(new KeyPair<>(c1, c2),c1.distancia(c2));
    }

    private List<Coordenada> getAllCoordenadas(){
        List<Coordenada> coordenadas=new ArrayList<>(maxX*maxY);
        for(int i=0;i<=maxX;i++){
            for(int j=0;j<=maxY;j++){
                coordenadas.add(new Coordenada(i,j));
            }
        }
        return coordenadas;
    }

    private Camino AStar(Coordenada origen, Coordenada destino,HashSet<Coordenada> bloqueos ){
        PriorityQueue<Camino> frontera = new PriorityQueue<>(Comparator.comparingDouble(c -> calcularCosto(c, destino)));
        HashSet<Coordenada> explorados = new HashSet<>();
        Camino nodo = new Camino(origen, origen, 0);
        frontera.add(nodo);
        boolean first=true;
        while (!frontera.isEmpty()) {
            nodo = frontera.poll();
            if (nodo.getDestino().equals(destino)) {
                return nodo;
            }
            if ( !bloqueos.contains(nodo.getDestino()) && distanceMap.containsKey(new KeyPair<>(nodo.getDestino(),destino))){
                return new Camino(origen, destino, nodo.getDistancia() + distanceMap.get(new KeyPair<>(nodo.getDestino(),destino)));
            }
            explorados.add(nodo.getDestino());
            for (Coordenada coor : getVecinos(nodo,bloqueos,first)) {
                first=false;
                Camino child = nodo.addCoordenada(coor);
                if (!explorados.contains(child.getDestino())&&!frontera.contains(child)) {
                    frontera.add(child);
                } else if (frontera.contains(child)) {
                    Camino old = frontera.stream().filter(c -> c.equals(child)).findFirst().orElse(null);
                    if (old != null && old.getDistancia() > child.getDistancia()) {
                        frontera.remove(old);
                        frontera.add(child);
                    }
                }
            }
        }
        return null;
    }

    //calcula la funcion costo de un camino
    private double calcularCosto(Camino camino,Coordenada destino){
        return camino.getDistancia()+camino.getDestino().distancia(destino);
    }

    private ArrayList<Coordenada> getVecinos(Camino nodo,HashSet<Coordenada> bloqueos, boolean first){
        ArrayList<Coordenada>vecinos=new ArrayList<>();
        Coordenada origen=nodo.getDestino();
        if (!first&&bloqueos.contains(origen)) {
            return vecinos;
        }
        if(origen.isYInteger()){
            //movimiento izquierda
            if(origen.getX()>0 ){
                vecinos.add(origen.izquierda());
            }
            //movimiento derecha
            if(origen.getX()<this.maxX ){
                vecinos.add(origen.derecha());
            }
        }
        if(origen.isXInteger()){
            //movimiento arriba
            if(origen.getY()<this.maxY ){
                vecinos.add(origen.arriba());
            }
            //movimiento abajo
            if(origen.getY()>0 ){
                vecinos.add(origen.abajo());
            }
        }
        return vecinos;
    }

    public void updateDistanceMap() {
        if (this.distancesCompressed != null) {
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addKeyDeserializer(KeyPair.class, new KeyPairDeserializer());
            mapper.registerModule(module);
            TypeReference<HashMap<KeyPair<Coordenada>, Double>> typeRef = new TypeReference<>() {};
            try {
                String decompressed = Utils.decompress(distancesCompressed);
                this.distancesCompressed = null;
                this.distanceMap = mapper.readValue(decompressed, typeRef);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } finally {
                this.distancesCompressed = null;
            }
        }
    }

    public void setBuild() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.distancesCompressed = Utils.compress(mapper.writeValueAsString(distanceMap));
            System.out.println("Se genero el JSON con un tamaño de "+this.distancesCompressed.length / 1024.0 / 1024.0+" MB ");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void clearBuild() {
        this.distancesCompressed = null;
    }

    public void destroy() {
        this.distanceMap=null;
        this.distancesCompressed = null;
    }

    @Getter
    @AllArgsConstructor
    private class Camino{
        private final Coordenada origen;
        private final Coordenada destino;
        private final double distancia;
        public Camino addCoordenada(Coordenada destino) {
            return new Camino(origen,destino,this.distancia + this.destino.distancia(destino));
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Camino camino)) return false;
            return Objects.equals(getOrigen(), camino.getOrigen()) && Objects.equals(getDestino(), camino.getDestino());
        }
        @Override
        public int hashCode() {
            return Objects.hash(getOrigen(), getDestino());
        }
    }
}
