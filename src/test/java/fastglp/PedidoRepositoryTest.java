package fastglp;

import fastglp.model.Camion;
import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;
import fastglp.repository.CiudadRepository;
import fastglp.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class PedidoRepositoryTest {
    @Autowired
    PedidoRepository pedidoRepository;
    @Autowired
    CiudadRepository ciudadRepository;
    private static final Logger logger = LoggerFactory.getLogger(PedidoRepositoryTest.class);
    @Autowired
    private MockMvc mockMvc;
    @Test
    public void registrarPedidoTest(){
        Date date = new Date();
        Ciudad ciudad = ciudadRepository.getById(1L);
        Pedido pedido = new Pedido(new Coordenada(0, 0), date, 10.2, 20.3, "aaa");
        pedido.setCiudad(ciudad); // Asigna la ciudad al pedido
        pedidoRepository.save(pedido);
        List<Pedido> pedidoList = pedidoRepository.findAll();
        assertEquals(1, pedidoList.size());
    }
    @Test
    public void testCargarPedidosEnMasa() throws Exception {
        logger.debug("Iniciando testCargarPedidosEnMasa");
        // Crear un MockMultipartFile
        MockMultipartFile file = new MockMultipartFile(
                "archivo",
                "nombreArchivo.txt",
                "text/plain",
                "contenido del archivo".getBytes()
        );

        // Realizar la solicitud POST
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/pedidos/cargarPedidosEnMasa")
                        .file(file))
                .andExpect(status().isOk()); // o el estado HTTP esperado
    }
}
