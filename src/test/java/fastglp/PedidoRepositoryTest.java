package fastglp;

import fastglp.model.Camion;
import fastglp.model.Coordenada;
import fastglp.model.Pedido;
import fastglp.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringBootTest
public class PedidoRepositoryTest {
    @Autowired
    PedidoRepository pedidoRepository;
    @Test
    public void registrarPedidoTest(){
        Date date = new Date();
        System.out.println(date);
        Pedido pedido = new Pedido(new Coordenada(0, 0),date, 10.2, 20.3, "aaa");
        pedidoRepository.save(pedido);
        List<Pedido> camionList = pedidoRepository.findAll();
        assertEquals( 1 , camionList.size());

    }
}
