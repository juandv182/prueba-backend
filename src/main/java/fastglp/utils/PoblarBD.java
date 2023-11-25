package fastglp.utils;

import fastglp.model.Ciudad;
import fastglp.service.CiudadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class PoblarBD {
    @Autowired
    private CiudadService ciudadService;
    public void poblar(){
        Ciudad ciudad;
        if(ciudadService.buscarPorId(1L)!=null){
            ciudadService.eliminar(1L);
        }
        ciudad = Utils.createCiudadOnly();
        long init = System.currentTimeMillis();
        System.out.println("Guardando ciudad");
        ciudadService.guardar(ciudad);
        System.out.println("Ciudad guardada en: " + (System.currentTimeMillis() - init) + " ms");
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
        init = System.currentTimeMillis();
        System.out.println("Guardando ciudad actualizada");
        ciudadService.guardar(ciudad);
        System.out.println("Ciudad guardada en: " + (System.currentTimeMillis() - init) + " ms");
    }
}
