package fastglp;

import fastglp.model.Ciudad;
import fastglp.service.CiudadService;
import fastglp.service.PedidoService;
import fastglp.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
public class PoblarBDTest {
    @Autowired
    private CiudadService ciudadService;
    private Ciudad ciudad;
    @BeforeEach
    public void crearCiudad(){
        if(ciudadService.buscarPorId(1L)!=null){
            ciudadService.eliminar(1L);
        }
        ciudad = Utils.createCiudadOnly();
        long init = System.currentTimeMillis();
        System.out.println("Guardando ciudad");
        ciudadService.guardar(ciudad);
        System.out.println("Ciudad guardada en: " + (System.currentTimeMillis() - init) + " ms");
    }

    @Test
    public void poblarBD(){
        //la llenamos con los pedidos y bloqueos de un mes en especifico
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 3,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 4,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 5,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 6,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 7,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 8,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 9,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 10,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 11,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 12,2023);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 1,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 2,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 3,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 4,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 5,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 6,2024);
        Utils.llenarBloqueosPedidosFromFiles(ciudad, 7,2024);
        long init = System.currentTimeMillis();
        System.out.println("Guardando ciudad actualizada");
        ciudadService.guardar(ciudad);
        System.out.println("Ciudad guardada en: " + (System.currentTimeMillis() - init) + " ms");
        //System.out.println("Se tiene una BD con "+pedidoService.listarPedidos().size());
    }
}
