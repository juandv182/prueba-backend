package fastglp.controller;

import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;
import fastglp.service.CiudadService;
import fastglp.service.PedidoService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private CiudadService ciudadService;
    private static final Logger logger = LoggerFactory.getLogger(PedidoController.class);

    @GetMapping(path = "", produces = "application/json")
    public ResponseEntity<List<Pedido>> obtenerPedidos() {
        List<Pedido> pedidos = null;
        pedidos = pedidoService.listarPedidos();
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public ResponseEntity<Pedido> obtenerPedidosPorId(@PathVariable long id) {
        log.info("Obtener pedido por ID: " + id);
        var pedidoOptional = pedidoService.obtenerPedidoPorId(id);
        if (pedidoOptional.isPresent()) {
            var pedido = pedidoOptional.get();
            return new ResponseEntity<>(pedido, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/porFecha")
    public ResponseEntity<List<Pedido>> listarPedidosPorFecha(
            @RequestParam("fechaInicio") @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaFin,
            @RequestParam("ciudadId") Long ciudadId) {

        Ciudad ciudad = ciudadService.buscarPorId(ciudadId);
        if (ciudad == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Pedido> pedidos = pedidoService.listarPedidosPorFecha(fechaInicio, fechaFin, ciudad);
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }
    @PostMapping(path = "", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Pedido> guardarPedido(@RequestBody Pedido pedido) {
        log.info("Pedido: " + pedido);
        pedidoService.guardarPedido(pedido);
        return new ResponseEntity<>(pedido, HttpStatus.CREATED);
    }
    @PostMapping("/cargarPedidosEnMasa")
    public ResponseEntity<?> cargarPedidosEnMasa(@RequestParam("archivo") MultipartFile archivo) {
        logger.debug("Entrando al método cargarPedidosEnMasa");
        if (archivo.isEmpty()) {
            return new ResponseEntity<>("El archivo está vacío", HttpStatus.BAD_REQUEST);
        }

        List<Pedido> pedidos = leerPedidos(archivo);
        if (pedidos.isEmpty()) {
            return new ResponseEntity<>("No se pudieron leer los pedidos del archivo", HttpStatus.BAD_REQUEST);
        }

        // Guardar todos los pedidos en la base de datos
        pedidoService.guardarPedidos(pedidos);
        return new ResponseEntity<>("Pedidos cargados con éxito", HttpStatus.CREATED);
    }

    private List<Pedido> leerPedidos(MultipartFile archivo) {
        List<Pedido> pedidos = new ArrayList<>();
        Date fechaInicioMes = new Date(); // Asumiendo que la fecha de inicio del mes se establece aquí

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

    @PutMapping(path = "/{id}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Void> actualizarPedido(@RequestBody Pedido pedido, @PathVariable long id) {
        log.info("Pedido: " + pedido);
        var pedidoOptional = pedidoService.obtenerPedidoPorId(id);
        if (pedidoOptional.isPresent()){
            pedido.setId(id);
            pedidoService.actualizarPedido(pedido);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable long id) {
        var pedidoOptional = pedidoService.obtenerPedidoPorId(id);
        if (pedidoOptional.isPresent()){
            pedidoService.eliminarPedido(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.info("::::handleValidationExceptionshandleValidationExceptions::::");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError)error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

}
