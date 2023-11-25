package fastglp;

import fastglp.model.Camion;
import fastglp.model.Coordenada;
import fastglp.repository.CamionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringBootTest
public class CamionRepositoryTest {
    @Autowired
    private CamionRepository camionRepository;


    @Test
    public void registrarCamionTest(){
        Camion camion = new Camion(25.3,3.2, new Coordenada(0, 0),2.5,2.2);

        System.out.println(camion);
        camionRepository.save(camion);

        List<Camion> camionList = camionRepository.findAll();
        assertEquals( 1 , camionList.size());
        System.out.println(camion);
        camionRepository.save(camion);

    }
}