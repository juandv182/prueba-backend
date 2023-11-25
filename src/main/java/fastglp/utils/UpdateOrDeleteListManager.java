package fastglp.utils;

import fastglp.model.AristaRuta;
import fastglp.model.PorcionPedido;
import fastglp.model.RegistroAlmacen;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateOrDeleteListManager {
    private final List<PorcionPedido>porcionPedidos= new ArrayList<>();
    private final List<RegistroAlmacen> registrosAlmacen = new ArrayList<>();
    private final List<AristaRuta>aristasRuta= new ArrayList<>();

    public void clear() {
        porcionPedidos.clear();
        registrosAlmacen.clear();
        aristasRuta.clear();
    }
}
