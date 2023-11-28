package fastglp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fastglp.Algorithm.ACO.Solution;
import fastglp.utils.UpdateOrDeleteListManager;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ciudad")
public class Ciudad {
    @Getter @Setter
    private String nombre;
    @Id
    @SequenceGenerator(name = "ciudad_sequence", sequenceName = "ciudad_sequence", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ciudad_sequence")
    @Column(name = "id")
    @Getter @Setter
    private Long id;
    @Getter @Setter
    @OneToMany(mappedBy = "ciudad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Camion> camiones;
    @Getter @Setter
    @OneToMany(mappedBy = "ciudad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pedido> pedidos;
    @Getter @Setter
    @OneToMany(mappedBy = "ciudad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bloqueo> bloqueos;
    //la ciudad tiene un maximo de longitud y latitud,(x y)
    //para que los camiones no se salgan de la ciudad
    @Getter
    @JsonIgnore
    private int maxX;
    @Getter
    @JsonIgnore
    private int maxY;
    @OneToMany(mappedBy = "ciudad", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    private List<Almacen>almacenes;
    @JsonIgnore
    private int totalAristas;
    @Transient
    @JsonIgnore
    @Getter
    private final UpdateOrDeleteListManager delete=new UpdateOrDeleteListManager();
    @Transient
    @JsonIgnore
    @Getter
    private final UpdateOrDeleteListManager update =new UpdateOrDeleteListManager();

    public Ciudad(String nombre, int maxX, int maxY) {
        this.nombre = nombre;
        this.maxX = maxX;
        this.maxY = maxY;
        this.camiones = new ArrayList<>();
        this.pedidos = new ArrayList<>();
        this.bloqueos = new ArrayList<>();
        this.almacenes=new ArrayList<>();
        this.totalAristas=(maxX-1)*maxY+(maxY-1)*maxX;
    }

    public ArrayList<Almacen> getAlmacenes(){
        return new ArrayList<>(this.almacenes);
    }

    public void addAlmacen(Almacen almacen) {
        if (this.almacenes == null) {
            this.almacenes = new ArrayList<>();
        }
        this.almacenes.add(almacen);
    }

    public void addCamion(Camion camion){
        this.camiones.add(camion);
    }

    public void addPedido(Pedido pedido){
        this.pedidos.add(pedido);
    }
    public void addPedido(ArrayList<Pedido> pedidos){
        this.pedidos.addAll(pedidos);
    }
    public void addBloqueo(Bloqueo bloqueo){
        this.bloqueos.add(bloqueo);
    }
    public void addBloqueo(ArrayList<Bloqueo> bloqueos){
        this.bloqueos.addAll(bloqueos);
    }
    public void removeCamion(Camion camion){
        this.camiones.remove(camion);
    }
    public void removePedido(Pedido pedido){
        this.pedidos.remove(pedido);
    }

    public void removeBloqueo(Bloqueo bloqueo){
        this.bloqueos.remove(bloqueo);
    }

    public void updateCamion(Camion camion){
        IntStream.range(0, this.camiones.size()).filter(i -> this.camiones.get(i).getId() == camion.getId()).forEach(i -> this.camiones.set(i, camion));
    }

    public void updatePedido(Pedido pedido){
        IntStream.range(0, this.pedidos.size()).filter(i -> this.pedidos.get(i).getId() == pedido.getId()).forEach(i -> this.pedidos.set(i, pedido));
    }

    public void updateBloqueo(Bloqueo bloqueo){
        IntStream.range(0, this.bloqueos.size()).filter(i -> this.bloqueos.get(i).getId() == bloqueo.getId()).forEach(i -> this.bloqueos.set(i, bloqueo));
    }

    public Camion getCamion(int id){
        return this.camiones.stream().filter(camione -> camione.getId() == id).findFirst().orElse(null);
    }

    public Pedido getPedido(int id){
        return this.pedidos.stream().filter(pedido -> pedido.getId() == id).findFirst().orElse(null);
    }

    public Bloqueo getBloqueo(int id){
        return this.bloqueos.stream().filter(bloqueo -> bloqueo.getId() == id).findFirst().orElse(null);
    }

    public boolean existeCamion(int id){
        return this.camiones.stream().anyMatch(camione -> camione.getId() == id);
    }

    public boolean existePedido(int id){
        return IntStream.range(0, this.pedidos.size()).anyMatch(i -> this.pedidos.get(i).getId() == id);
    }

    public boolean existeBloqueo(int id){
        return IntStream.range(0, this.bloqueos.size()).anyMatch(i -> this.bloqueos.get(i).getId() == id);
    }

    //devuelve el camino mas corto entre dos coordenadas considerando los bloqueos
    //utilizando A*
    public Camino AStar(Coordenada origen, Coordenada destino, Date fechaInicio,
                        double currentPetroleo, double gastoXkm, Coordenada prevCoordenada){
        assert destino.isYInteger()&&destino.isXInteger():"la coordenada destino no es entera";
        PriorityQueue<Camino> frontera = new PriorityQueue<>(Comparator.comparingDouble(c -> calcularCosto(c, destino)));
        HashSet<Coordenada> explorados = new HashSet<>();
        Camino nodo = new Camino(origen, origen, new ArrayList<>(Collections.singletonList(origen)), fechaInicio);
        frontera.add(nodo);
        boolean first=true;
        while (!frontera.isEmpty()) {
            nodo = frontera.poll();
            if (nodo.getDestino().equals(destino)) {
                return nodo;
            }
            if (calcularCosto(nodo, destino) * gastoXkm > currentPetroleo) {
                return null;
            }
            explorados.add(nodo.getDestino());

            for (Coordenada coor : getVecinos(nodo, first, prevCoordenada)) {
                first=false;
                ArrayList<Coordenada> coordenadas = new ArrayList<>(nodo.getCoordenadas());
                coordenadas.add(coor);
                Camino child = new Camino(nodo.getOrigen(), coor, coordenadas, fechaInicio);
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

    //consigue las coordenadas vecinas de un nodo

    private ArrayList<Coordenada> getVecinos(Camino nodo,boolean first,Coordenada prevCoordenada){
        ArrayList<Coordenada>vecinos=new ArrayList<>();
        Coordenada origen=nodo.getDestino();
        Date fecha=nodo.getFechaFin();
        if (!libreDeBloqueos(origen, fecha) && (!first || (prevCoordenada != null && !prevCoordenada.equals(origen) && libreDeBloqueos(prevCoordenada, fecha)))){
            if(first){
                vecinos.add(prevCoordenada);
            }
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

    private boolean libreDeBloqueos(Coordenada origen, Date fecha){
        //verificar si el camino pasa por algun bloqueo
        for (Bloqueo bloqueo : this.bloqueos) {
            if (bloqueo.isLocked(origen,fecha)) {
                return false;
            }
        }
        return true;
    }

    private int getNumeroDeBloqueos(Date fecha){
        return this.bloqueos.stream().filter(b->b.getFechaInicio().compareTo(fecha)<=0 && b.getFechaFin().compareTo(fecha)>0).mapToInt(Bloqueo::size).sum();
    }

    public double getBlockFactor(Date date){
        return 1+ (double) this.getNumeroDeBloqueos(date) /this.totalAristas;
    }

    public Almacen getAlmacenMasCercano(Coordenada coordenada,DistanceGraph distanceGraph){
        double distancia=1e200;
        Almacen almacen=null;
        for (Almacen a:this.getAlmacenes()) {
            double d=distanceGraph.getDistance(a.getCoordenada(),coordenada);
            if(d<distancia){
                distancia=d;
                almacen=a;
            }
        }
        return almacen;
    }

    public Almacen getAlmacenMasCercano(Coordenada coordenada,Coordenada destino,DistanceGraph distanceGraph){
        double distancia=1e200;
        Almacen almacen=null;
        for (Almacen a:this.getAlmacenes()) {
            double d=distanceGraph.getDistance(a.getCoordenada(),coordenada)
                    +distanceGraph.getDistance(a.getCoordenada(),destino);
            if(d<distancia){
                distancia=d;
                almacen=a;
            }
        }
        return almacen;
    }
    public Almacen getAlmacenMasCercano(Coordenada coordenada,Coordenada destino,ArrayList<Almacen>almacenes, DistanceGraph distanceGraph){
        double distancia=1e200;
        Almacen almacen=null;
        for (Almacen a:almacenes) {
            double d=distanceGraph.getDistance(a.getCoordenada(),coordenada)
                    +distanceGraph.getDistance(a.getCoordenada(),destino);
            if(d<distancia){
                distancia=d;
                almacen=a;
            }
        }
        return almacen;
    }

    private void organizarCamiones(ArrayList<Camion>camiones){
        //quitar todos los camiones que tienen ruta vacia
        camiones.removeIf(c->c.solution.pedidos.isEmpty());
        camiones.sort(Comparator.comparingLong(c->c.solution.pedidos.get(0).getPedido().getFechaLimite().getTime()));
    }
    @JsonIgnore
    public Almacen getAlmacenPrincipal(){
        return this.almacenes.stream().filter(Almacen::isPrincipal).findFirst().orElse(null);
    }

    private void atenderPedido(Solution s,PorcionPedido p, Camino camino,Camion c){
        s.currentFecha=camino.getFechaFin();
        s.currentCoordenada=camino.getDestino();
        c.addRuta(new AristaRuta(c,p,camino,
                s.currentGLP,
                (s.currentGLP-=p.getGlp()),
                s.currentPetroleo,
                (s.currentPetroleo-=camino.getDistancia()*s.currentPeso/ Camion.getConsumo())));
        s.currentPeso= c.getPesoBase()+s.currentGLP*0.5;
        p.setCamion(c);
        p.setFechaEntrega(s.currentFecha);
        p.setAsignado(true);
        assert p.getGlp()<=c.getCapacidadGLP():"el pedido tiene mas GLP que la capacidad del camion";
        p.setFechaEntrega(new Date(Math.min(s.currentFecha.getTime(),p.getFechaLimite().getTime()-2000L)));
        s.pedidos.remove(0);
        //if(s.currentFecha.after(p.getPedido().getFechaLimite())){
        //    System.out.println("--------------------------------------------------");
        //    System.out.println("Era "+camino.getFechaInicio());
        //    System.out.println("se entrego el pedido despues de la fecha de entrega");
        //    System.out.println("Camion: "+c.getCapacidadGLP());
        //    System.out.println("Pedido: " + p.getPedido().getId() + "\n"+"Cliente: "+p.getPedido().getCliente()+"\n"+"Coordenada: "+p.getPedido().getCoordenada());
        //    System.out.println("Fecha de entrega entregada: "+s.currentFecha);
        //    System.out.println("Deadline del pedido: "+p.getPedido().getFechaLimite());
        //    System.out.println("Se ha perdido por : "+(s.currentFecha.getTime()-p.getPedido().getFechaLimite().getTime())/ 3600000.0+"horas");
        //    c.getRuta().forEach(a->System.out.println(a.getCamino().getOrigen()+" "+a.getCamino().getDestino()+"   "+a.getTipo()+"  "
        //            +(a.getTipo().equals("pedido")?(a.getPedido()).getGlp():a.comment)));
        //    System.out.println("--------------------------------------------------\nPedidos");
        //    s.pedidos.forEach(System.out::println);
        //    this.almacenes.forEach(System.out::println);
        //    throw new RuntimeException("HA PERDIDO EL PEDIDO PORQUE SE ENTREGO DESPUES DE LA FECHA DE ENTREGA");
        //}else{
        //    p.setFechaEntrega(s.currentFecha);
        //    s.pedidos.remove(0);
        //}
    }

    public void buildRutas(ArrayList<Solution> best,Date fecha, DistanceGraph distanceGraph){
        best.forEach(s-> {
            s.prepareToBuildRoutes();
            s.camion.solution=s;
        });
        ArrayList<Camion>currentCamiones=new ArrayList<>(this.camiones);
        organizarCamiones(currentCamiones);
        Date fechaFinal=best.stream().flatMap(s -> s.pedidos.stream()).max(Comparator.comparing(pp->pp.getPedido().getFechaLimite())).map(pp->pp.getPedido().getFechaLimite()).orElse(fecha);
        this.almacenes.forEach(a->a.generarCalendarioGLP(fecha,fechaFinal));
        assert currentCamiones.stream().noneMatch(c->c.solution!=c.solution.camion.solution):"algun camion tiene una solucion que no es la solucion del camion";
        while(!currentCamiones.isEmpty()){
            ArrayList<Camion>needReload=new ArrayList<>();
            for(Camion c:currentCamiones){
                Solution s=c.solution;
                PorcionPedido p=s.pedidos.get(0);
                Almacen a2=getAlmacenMasCercano(p.getPedido().getCoordenada(),distanceGraph);
                double gastoPetroleo=s.currentPeso*distanceGraph.getDistance(s.currentCoordenada,p.getPedido().getCoordenada())/ Camion.getConsumo()
                        + (s.currentPeso-.5*p.getGlp())*(distanceGraph.getDistance(p.getPedido().getCoordenada(),a2.getCoordenada())*1.4)/ Camion.getConsumo();
                Camino camino=(s.currentGLP<p.getGlp()||s.currentPetroleo<gastoPetroleo)
                        ?null: this.AStar(s.currentCoordenada,p.getPedido().getCoordenada(), s.currentFecha,s.currentPetroleo,
                        s.currentPeso/ Camion.getConsumo(),c.getPenultimaCoordenada());
                if(camino==null) {
                    needReload.add(c);
                    continue;
                }
                assert camino.getDestino().equals(p.getPedido().getCoordenada()):"el destino del camino no es el pedido";

                atenderPedido(s,p,camino,c);
            }
            while(!needReload.isEmpty()){
                Camion c=needReload.get(0);
                Solution s=c.solution;
                PorcionPedido p=s.pedidos.get(0);
                boolean leFaltaGLP=s.currentGLP<p.getGlp();
                if(!leFaltaGLP){
                    recargarPetroleo(c,s,p.getPedido().getCoordenada(), distanceGraph);
                    needReload.remove(0);
                    continue;
                }
                // si le falta GLP
                if(recargarGLP(c,s,p.getPedido().getCoordenada(), p.getGlp()-s.currentGLP, distanceGraph)){
                    needReload.remove(0);
                    continue;
                }
                //si no se pudo recargar GLP se tratara de recargar petroleo
                recargarPetroleo(c,s,p.getPedido().getCoordenada(),distanceGraph);
            }
            organizarCamiones(currentCamiones);
        }
        //llenar los camiones en el almacen principal
        Almacen almacen=this.getAlmacenPrincipal();
        for(Camion c:this.camiones){
            Solution s=c.solution;
            if(s.currentGLP==c.getCapacidadGLP()){
                continue;
            }
            if(recargarGLP(c,s,almacen.getCoordenada(),c.getCapacidadGLP()-s.currentGLP, distanceGraph)){
                continue;
            }
            recargarPetroleo(c,s,almacen.getCoordenada(),distanceGraph);
            if(s.currentGLP==c.getCapacidadGLP()){
                continue;
            }
            recargarGLP(c,s,almacen.getCoordenada(),c.getCapacidadGLP()-s.currentGLP, distanceGraph);
        }
        //llevar los camiones a un almacen aleatorio
        int nAlmacenes=this.almacenes.size();
        ArrayList<Almacen>almacenes=this.getAlmacenes();
        for (Camion c : camiones) {
            Solution s=c.solution;
            Almacen a=almacenes.get(1);
            if(!recargarGLP(c,s,a.getCoordenada(),0, distanceGraph)){
                recargarPetroleo(c,s,a.getCoordenada(),distanceGraph);
                recargarGLP(c,s,a.getCoordenada(),0, distanceGraph);
            }
        }
    }

    private void recargarPetroleo(Camion c, Solution s,Coordenada destino, DistanceGraph distanceGraph){
        ArrayList<Almacen>almacenes=this.getAlmacenes();
        Camino camino;
        Almacen first = almacenes.get(0);
        while(!almacenes.isEmpty()){
            Almacen a=getAlmacenMasCercano(s.currentCoordenada,destino,almacenes, distanceGraph);
            camino=this.AStar(s.currentCoordenada,a.getCoordenada(),s.currentFecha,25.0,
                    s.currentPeso/ Camion.getConsumo(),c.getPenultimaCoordenada());
            if(camino==null){
                almacenes.remove(a);
            }else{
                AristaRuta aristaRuta=new AristaRuta(c,a,camino,
                        s.currentGLP,
                        (s.currentGLP=s.currentGLP+a.reducirGLP(s.currentFecha,c.getCapacidadGLP()-s.currentGLP,false)),
                        s.currentPetroleo,
                        (s.currentPetroleo=Camion.getCapacidadPetroleo()));
                aristaRuta.comment="POR recargar petroleo";
                c.addRuta(aristaRuta);
                s.currentFecha=camino.getFechaFin();
                s.currentCoordenada=camino.getDestino();
                return;
            }
        }
        //throw new RuntimeException("camino de recarga por petroleo ha fallado");
        s.currentFecha=new Date(s.currentFecha.getTime()+60000L);
    }

    private boolean recargarGLP(Camion c, Solution s,Coordenada destino,double glp, DistanceGraph distanceGraph){
        ArrayList<Almacen>almacenes=this.getAlmacenes();
        Camino camino;
        while(!almacenes.isEmpty()){
            Almacen a=getAlmacenMasCercano(s.currentCoordenada,destino,almacenes,distanceGraph);
            camino=this.AStar(s.currentCoordenada,a.getCoordenada(),s.currentFecha,s.currentPetroleo,
                    s.currentPeso/ Camion.getConsumo(),c.getPenultimaCoordenada());
            if(camino==null||a.getGLP(camino.getFechaFin())<glp){
                almacenes.remove(a);
            }else{
                AristaRuta aristaRuta=new AristaRuta(c,a,camino,
                        s.currentGLP,
                        (s.currentGLP=s.currentGLP+a.reducirGLP(s.currentFecha,c.getCapacidadGLP()-s.currentGLP,false)),
                        s.currentPetroleo,
                        (s.currentPetroleo=Camion.getCapacidadPetroleo()));
                assert s.currentGLP<=c.getCapacidadGLP(): "se ha recargado mas GLP del que puede almacenar el camion";
                aristaRuta.comment="POR recargar GLP";
                c.addRuta(aristaRuta);
                s.currentFecha=camino.getFechaFin();
                s.currentCoordenada=camino.getDestino();
                s.currentPeso= c.getPesoBase()+s.currentGLP*0.5;
                return true;
            }
        }
        return false;
    }

    public Ciudad getClearCity(Date fecha) {
        Ciudad ciudad=new Ciudad(this.nombre,this.maxX,this.maxY);
        ciudad.setId(this.id);
        ciudad.camiones=new ArrayList<>(this.camiones);
        ciudad.pedidos=this.pedidos.stream().filter(p->p.getFechaSolicitud().compareTo(fecha)<=0).collect(ArrayList::new,ArrayList::add,ArrayList::addAll);
        ciudad.bloqueos=this.bloqueos.stream().filter(b->b.getFechaFin().compareTo(fecha)>0).collect(ArrayList::new,ArrayList::add,ArrayList::addAll);
        ciudad.almacenes=this.almacenes;
        ciudad.clear();
        return ciudad;
    }

    private void clear() {
        //camiones listo para JSON
        //pedidos listo para JSON
        //almacenes listo para JSON
    }
}