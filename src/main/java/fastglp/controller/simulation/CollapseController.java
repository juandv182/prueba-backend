package fastglp.controller.simulation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fastglp.model.Ciudad;
import fastglp.model.DistanceGraph;
import fastglp.service.DistanceGraphService;
import fastglp.utils.Estadisticas;
import fastglp.utils.FastGLPSimulation;
import fastglp.utils.Utils;
import fastglp.utils.pdf.PdfGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
public class CollapseController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private DistanceGraphService distanceGraphService;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    private FastGLPSimulation createSimulation() {
        Date fechaInicio = Utils.parseFecha("01/04/2023 00");
        //4 - 12 | 8 meses
        Date fechaFin = new Date(fechaInicio.getTime() + 20736000000L);
        Ciudad ciudad = Utils.createMockCiudadInRange(4,12,2023);
        FastGLPSimulation simulation = new FastGLPSimulation(ciudad,fechaInicio,fechaFin);
        simulation.setType("collapse");
        simulation.setDistanceGraph(new DistanceGraph(ciudad, 6.0));
        simulation.setDistanceGraphService(distanceGraphService);
        return simulation;
    }

    @MessageMapping("/execute-collapse")
    public void executeCollapse() {
        if(isRunning.compareAndSet(false,true)){
            stopRequested.set(false);
            long time = System.currentTimeMillis();
            boolean isLast = false;
            FastGLPSimulation currentsimu = createSimulation();
            ObjectMapper mapper = new ObjectMapper();
            Reporte reporte;
            while (!isLast){
                if (stopRequested.compareAndSet(true, false)) {
                    System.out.println("Simulacion colapso detenida en: " + (System.currentTimeMillis() - time) + "ms");
                    break;
                }
                isLast=!currentsimu.optimize();
                reporte = new Reporte(null,currentsimu.getEstadisticas(),false);
                messagingTemplate.convertAndSend("/response/response-collapse", reporte);
                try {
                    currentsimu.getEstadisticas().setLastSimulation(mapper.writeValueAsString(currentsimu));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Simulacion en: " + (System.currentTimeMillis() - time) + "ms");
            //enviando reporte
            System.out.println("Enviando reporte");
            Estadisticas estadisticas = currentsimu.getEstadisticas();
            reporte = new Reporte(null,estadisticas,true);
            try {
                reporte.setReporte(
                        Utils.convertToUnsignedByteArray(
                                PdfGenerator.generatePdf(estadisticas, "src/generated/test.pdf")
                        )
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            messagingTemplate.convertAndSend("/response/response-collapse", reporte);
            isRunning.set(false);
        }
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    private static class Reporte{
        private int[] reporte;
        private Estadisticas estadisticas;
        private boolean last;
    }

    @MessageMapping("/stop-collapse")
    public void stopWeek() {
        stopRequested.set(isRunning.get());
    }
}
