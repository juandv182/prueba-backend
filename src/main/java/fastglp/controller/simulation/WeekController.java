package fastglp.controller.simulation;

import fastglp.model.*;
import fastglp.service.BloqueoService;
import fastglp.service.DistanceGraphService;
import fastglp.service.PedidoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
public class WeekController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private DistanceGraphService distanceGraphService;
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private BloqueoService bloqueoService;
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
    public void executeWeek(@RequestParam(value = "pedidos", required = false) MultipartFile pedidos,
                            @RequestParam(value = "bloqueos", required = false) MultipartFile bloqueos,
                            @RequestParam(value = "averias", required = false) MultipartFile averias,
                            @RequestParam(value = "otroArchivo", required = false) MultipartFile otroArchivo,
                            @RequestParam("fechaInicio") String fechaInicio) {

        // Procesar archivos y fecha
        if (pedidos != null) {
            // Procesar archivo de pedidos
            List<Pedido> pedidosObt = leerPedidos(pedidos);
            pedidoService.guardarPedidos(pedidosObt);

        }
        if (bloqueos != null) {
            // Procesar archivo de bloqueos
            List<Bloqueo> bloqueossObt = leerBloqueos(bloqueos);
            bloqueoService.guardarBloqueos(bloqueossObt);

        }
        if (averias != null) {
            // Procesar archivo de averías
        }
        if (otroArchivo != null) {
            // Procesar el otro archivo
        }

        if(isRunning.compareAndSet(false,true)){
            System.out.println("Iniciando simulacion semana");
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
                isLast= currentsimu.optimize();
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
    private List<Pedido> leerPedidos(MultipartFile archivo) {
        List<Pedido> pedidos = new ArrayList<>();
        Date fechaInicioMes = new Date();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Asumiendo que la línea sigue el formato "11d13h31m:45,43,c-167,9m3,36h"
                String[] parts = line.split(":");
                String[] fechaYHora = parts[0].split("d|h|m");
                String[] datosPedido = parts[1].split(",");

                // Parsear la fecha y hora
                int dias = Integer.parseInt(fechaYHora[0]);
                int horas = Integer.parseInt(fechaYHora[1]);
                int minutos = Integer.parseInt(fechaYHora[2]);

                // Calcular la fecha y hora del pedido
                Calendar cal = Calendar.getInstance();
                cal.setTime(fechaInicioMes);
                cal.add(Calendar.DAY_OF_MONTH, dias - 1); // Ajustar por el día del mes
                cal.add(Calendar.HOUR_OF_DAY, horas);
                cal.add(Calendar.MINUTE, minutos);

                Date fechaPedido = cal.getTime();

                // Parsear los datos del pedido
                Coordenada coord = new Coordenada(Double.parseDouble(datosPedido[0]), Double.parseDouble(datosPedido[1]));
                String idCliente = datosPedido[2];
                double cantidad = Double.parseDouble(datosPedido[3].replace("m3", ""));
                double duracion = Double.parseDouble(datosPedido[4].replace("h", ""));

                // Crear y añadir el pedido a la lista
                Pedido pedido = new Pedido(coord, fechaPedido, duracion, cantidad, idCliente);
                pedidos.add(pedido);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.printf("Se han leido %d pedidos%n", pedidos.size());
        return pedidos;
    }
    private static long toMillis(String date) {
        String[] parts = date.split("[dhm]");
        return (Long.parseLong(parts[0]) - 1) * 86400000 + Long.parseLong(parts[1]) * 3600000 + Long.parseLong(parts[2]) * 60000;
    }
    private static Date getAdjustedDate(Date fechaInicioMes, String date) {
        return new Date(fechaInicioMes.getTime() + toMillis(date));
    }
    public static ArrayList<Bloqueo> leerBloqueos(MultipartFile archivo) {
        ArrayList<Bloqueo> bloqueos = new ArrayList<>();
        Date fechaInicioMes = new Date();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                String[] fechas = parts[0].split("-");
                Bloqueo bloqueo = new Bloqueo(getAdjustedDate(fechaInicioMes, fechas[0]), getAdjustedDate(fechaInicioMes, fechas[1]));
                String[] coords = parts[1].split(",");
                for (int j = 0; j < coords.length - 1; j += 2) {
                    bloqueo.addCoordenada(new Coordenada(Double.parseDouble(coords[j]), Double.parseDouble(coords[j + 1])));
                }
                bloqueos.add(bloqueo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("Se han leido %d bloqueos%n", bloqueos.size());
        return bloqueos;
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
    @MessageMapping("/status-week")
    public void statusWeek() {
        messagingTemplate.convertAndSend("/status/status-week", isRunning.get()? "running" : "stopped");
    }
}
