package fastglp;

import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;
import fastglp.repository.CiudadRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class CiudadRepositoryTest {
    @Autowired
    CiudadRepository ciudadRepository;
    @Test
    public void registrarPedidoTest(){
        Date date = new Date();
        //System.out.println(date);
        Ciudad ciudad = new Ciudad("SkyLines", 70, 50);
        ciudadRepository.save(ciudad);
        List<Ciudad> ciudadList = ciudadRepository.findAll();
        assertEquals( 1 , ciudadList.size());

    }
}
