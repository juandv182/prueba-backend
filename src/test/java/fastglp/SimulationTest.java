package fastglp;

import com.fasterxml.jackson.databind.ObjectMapper;
import fastglp.model.*;
import fastglp.service.DistanceGraphService;
import fastglp.model.DistanceGraph;
import fastglp.utils.FastGLPSimulation;
import fastglp.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Date;

@SpringBootTest
public class SimulationTest {
    @Autowired
    private DistanceGraphService distanceGraphService;
    @Test
    public void testWeekSimulation(){
        Date fechaInicio = Utils.parseFecha("01/04/2023 00");
        //7 dias
        Date fechaFin = new Date(fechaInicio.getTime() + 604800000L);
        Ciudad ciudad = Utils.createMockCiudad(4,2023);
        FastGLPSimulation simulation = new FastGLPSimulation(ciudad,fechaInicio,fechaFin);
        simulation.setType("7days");
        simulation.setDistanceGraph(new DistanceGraph(ciudad, 6.0));
        simulation.setDistanceGraphService(distanceGraphService);
        long inicio = System.currentTimeMillis();
        int i=0;
        long duracionMax=0;
        boolean isLast = false;
        while (!isLast) {
            if(i%70==0){
                System.out.println("Siendo: "+fechaInicio);
            }
            long init2=System.nanoTime();
            isLast = !simulation.optimize();
            duracionMax=Math.max(duracionMax,System.nanoTime()-init2);
            fechaInicio = new Date(fechaInicio.getTime() + 300000L);
            i++;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new java.io.File("ACOAlgorithmTest.json"), simulation);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("Tiempo de ejecucion: " + (System.currentTimeMillis() - inicio) / 1000 + " segundos");
            System.out.println("Tiempo de iteración maximo: " + duracionMax / 1000000 + "mili segundos");
        }
    }
}
