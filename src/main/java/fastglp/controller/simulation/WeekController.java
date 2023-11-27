package fastglp.controller.simulation;

import fastglp.model.Ciudad;
import fastglp.service.DistanceGraphService;
import fastglp.model.DistanceGraph;
import fastglp.utils.Estadisticas;
import fastglp.utils.FastGLPSimulation;
import fastglp.utils.Utils;
import fastglp.utils.pdf.PdfGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
public class WeekController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private DistanceGraphService distanceGraphService;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    private FastGLPSimulation createSimulation() {
        Date fechaInicio = Utils.parseFecha("01/04/2023 00");
        //7 dias
        Date fechaFin = new Date(fechaInicio.getTime() + 604800000L);
        Ciudad ciudad = Utils.createMockCiudad(4,2023);
        FastGLPSimulation simulation = new FastGLPSimulation(ciudad,fechaInicio,fechaFin);
        simulation.setType("7days");
        simulation.setDistanceGraph(new DistanceGraph(ciudad, 6.0));
        simulation.setDistanceGraphService(distanceGraphService);
        return simulation;
    }

    @MessageMapping("/execute-week")
    public void executeWeek() {
        if(isRunning.compareAndSet(false,true)){
            stopRequested.set(false);
            long time = System.currentTimeMillis();
            boolean isLast = false;
            FastGLPSimulation currentsimu = createSimulation();
            int totalCiclos=0;
            while (!isLast){
                totalCiclos++;
                if (stopRequested.compareAndSet(true, false)) {
                    System.out.println("Simulacion detenida en: " + (System.currentTimeMillis() - time) + "ms");
                    break;
                }
                //currentsimu.optimize();
                isLast=!currentsimu.optimize();
                currentsimu.setLast(false);
                messagingTemplate.convertAndSend("/response/response-week", currentsimu);
                //System.out.println("Mensaje enviado en " + (System.currentTimeMillis() - time) + "ms");
            }
            System.out.println("Simulacion en: " + (System.currentTimeMillis() - time) + "ms");
            System.out.println("Total de ciclos: " + totalCiclos);
            //tiempo promedio
            System.out.println("Tiempo promedio por ciclo: " + (System.currentTimeMillis() - time)/totalCiclos + "ms");
            //enviando reporte
            System.out.println("Enviando reporte");
            Estadisticas estadisticas = currentsimu.getEstadisticas();
            currentsimu=null;
            Reporte reporte = new Reporte(estadisticas);
            try {
                reporte.setReporte(
                    Utils.convertToUnsignedByteArray(
                            PdfGenerator.generatePdf(estadisticas, "src/generated/test.pdf")
                    )
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            messagingTemplate.convertAndSend("/response/response-week", reporte);
            isRunning.set(false);
        }
    }
    @AllArgsConstructor
    @Getter @Setter
    private static class Reporte{
        private int[] reporte;
        private final Estadisticas estadisticas;
        private final boolean last=true;
        public Reporte(Estadisticas estadisticas){
            this.estadisticas=estadisticas;
        }
    }

    @MessageMapping("/stop-week")
    public void stopWeek() {
        stopRequested.set(isRunning.get());
    }
}
