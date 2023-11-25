package fastglp.service;

import fastglp.model.Ciudad;
import fastglp.model.Pedido;
import fastglp.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
@Service
public class PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Optional<Pedido> obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    public Long guardarPedido(Pedido pedido) {
        pedidoRepository.save(pedido);
        return pedido.getId();
    }


    public void actualizarPedido(Pedido pedido) {
        pedidoRepository.save(pedido);
    }


    public void eliminarPedido(Long id) {
        pedidoRepository.deleteById(id);
    }

    public ArrayList<Pedido> listarPedidosPorFecha(Date fechaInicio, Date fechaFin, Ciudad ciudad) {
        ArrayList<Pedido> pedidos = new ArrayList<>(pedidoRepository.
                findByFechaSolicitudBetween(fechaInicio, fechaFin,ciudad.getId()));
        pedidos.forEach(p->{
            p.setPorciones(new ArrayList<>());
            p.setCiudad(ciudad);
        });
        return pedidos;
    }
}