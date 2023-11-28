package fastglp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fastglp.model.*;
import fastglp.service.DistanceGraphService;
import fastglp.model.DistanceGraph;
import fastglp.utils.FastGLPSimulation;
import fastglp.utils.Utils;
import fastglp.utils.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

@SpringBootTest
public class SimulationTest {
    @Autowired
    private DistanceGraphService distanceGraphService;
    @Test
    public void testWeekSimulation(){
        Date fechaInicio = Utils.parseFecha("01/04/2023 00");
        //7 dias 604800000L
        // medio dia 36400000L
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
            isLast = simulation.optimize();
            duracionMax=Math.max(duracionMax,System.nanoTime()-init2);
            fechaInicio = new Date(fechaInicio.getTime() + 300000L);
            i++;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String dest = "src/generated/test.pdf";
            byte[] pdfBytes = PdfGenerator.generatePdf(simulation.getEstadisticas(), dest);
            mapper.writeValue(new java.io.File("src/generated/ACOAlgorithmTest.json"), simulation);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(pdfBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("Tiempo de ejecucion: " + (System.currentTimeMillis() - inicio) / 1000 + " segundos");
            System.out.println("Tiempo de iteración maximo: " + duracionMax / 1000000 + "mili segundos");
        }
    }
    @Test
    public void testCollapseSimulation(){
        Date fechaInicio = Utils.parseFecha("01/04/2023 00");
        //4 - 12 | 8 meses
        Date fechaFin = new Date(fechaInicio.getTime() + 20736000000L);
        Ciudad ciudad = Utils.createMockCiudadInRange(4,12,2023);


        FastGLPSimulation simulation = new FastGLPSimulation(ciudad,fechaInicio,fechaFin);
        simulation.setType("collapse");
        simulation.setDistanceGraph(new DistanceGraph(ciudad, 6.0));
        simulation.setDistanceGraphService(distanceGraphService);
        ObjectMapper mapper = new ObjectMapper();
        long inicio = System.currentTimeMillis();
        int i=0;
        long duracionMax=0;
        boolean isLast = false;
        while (!isLast) {
            if(i%70==0){
                System.out.println("Siendo: "+fechaInicio);
            }
            long init2=System.nanoTime();
            isLast = simulation.optimize();
            duracionMax=Math.max(duracionMax,System.nanoTime()-init2);
            fechaInicio = new Date(fechaInicio.getTime() + 300000L);
            i++;
            try {
                simulation.getEstadisticas().setLastSimulation(mapper.writeValueAsString(simulation));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            String dest = "src/generated/test.pdf";
            byte[] pdfBytes = PdfGenerator.generatePdf(simulation.getEstadisticas(), dest);
            mapper.writeValue(new java.io.File("src/generated/ACOAlgorithmTest.json"), simulation);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(pdfBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("Tiempo de ejecucion: " + (System.currentTimeMillis() - inicio) / 1000 + " segundos");
            System.out.println("Tiempo de iteración maximo: " + duracionMax / 1000000 + "mili segundos");
        }
    }
}
