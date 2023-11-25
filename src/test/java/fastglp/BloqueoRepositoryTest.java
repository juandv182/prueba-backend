package fastglp;

import fastglp.model.Bloqueo;
import fastglp.model.Ciudad;
import fastglp.model.Coordenada;
import fastglp.repository.BloqueoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringBootTest
public class BloqueoRepositoryTest {
    @Autowired
    private BloqueoRepository bloqueoRepository;
    @Test
    public void registrarBloqueoTest(){
        Date date = new Date();
        Bloqueo bloqueo= new Bloqueo(date,date);
        bloqueo.addCoordenada(new Coordenada(0,0));
        bloqueoRepository.save(bloqueo);
        List<Bloqueo> list = bloqueoRepository.findAll();
        assertEquals( 1 , list.size());
    }
}
