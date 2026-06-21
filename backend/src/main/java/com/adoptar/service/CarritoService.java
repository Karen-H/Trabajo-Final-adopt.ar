package com.adoptar.service;

import com.adoptar.dto.response.CarritoGrupoResponse;
import com.adoptar.dto.response.CarritoItemResponse;
import com.adoptar.entity.CarritoItem;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.EstadoItem;
import com.adoptar.enums.UserProfile;
import com.adoptar.repository.CarritoItemRepository;
import com.adoptar.repository.ItemTiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoItemRepository carritoItemRepository;
    private final ItemTiendaRepository itemTiendaRepository;

    @Transactional
    public void agregar(User adoptante, Long itemId, Integer cantidad) {
        if (adoptante.getActiveProfile() != UserProfile.ADOPTANTE) {
            throw new IllegalArgumentException("Solo podés comprar con el perfil de adoptante activo");
        }
        ItemTienda item = itemTiendaRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (item.isEliminado() || item.getEstado() != EstadoItem.APROBADO) {
            throw new IllegalArgumentException("Este ítem no está disponible");
        }
        if (item.getRescatista().getId().equals(adoptante.getId())) {
            throw new IllegalArgumentException("No podés comprar tus propios items");
        }

        CarritoItem existente = carritoItemRepository.findByAdoptanteAndItem(adoptante, item).orElse(null);
        int cantidadFinal = (existente != null ? existente.getCantidad() : 0) + cantidad;
        if (cantidadFinal > item.getStock()) {
            throw new IllegalArgumentException("No hay suficiente stock disponible");
        }

        if (existente != null) {
            existente.setCantidad(cantidadFinal);
            carritoItemRepository.save(existente);
        } else {
            carritoItemRepository.save(CarritoItem.builder()
                    .adoptante(adoptante)
                    .item(item)
                    .cantidad(cantidadFinal)
                    .build());
        }
    }

    @Transactional
    public void actualizarCantidad(User adoptante, Long itemId, Integer cantidad) {
        CarritoItem carritoItem = getCarritoItemDelAdoptante(adoptante, itemId);
        if (cantidad > carritoItem.getItem().getStock()) {
            throw new IllegalArgumentException("No hay suficiente stock disponible");
        }
        carritoItem.setCantidad(cantidad);
        carritoItemRepository.save(carritoItem);
    }

    @Transactional
    public void eliminar(User adoptante, Long itemId) {
        ItemTienda item = itemTiendaRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        carritoItemRepository.deleteByAdoptanteAndItem(adoptante, item);
    }

    @Transactional(readOnly = true)
    public List<CarritoGrupoResponse> listar(User adoptante) {
        List<CarritoItem> items = carritoItemRepository.findByAdoptante(adoptante);

        Map<User, List<CarritoItem>> porRescatista = items.stream()
                .collect(Collectors.groupingBy(ci -> ci.getItem().getRescatista()));

        return porRescatista.entrySet().stream()
                .map(entry -> {
                    User rescatista = entry.getKey();
                    List<CarritoItemResponse> itemsResponse = entry.getValue().stream()
                            .sorted(Comparator.comparing(CarritoItem::getCreadoEn))
                            .map(this::toItemResponse)
                            .toList();
                    BigDecimal total = entry.getValue().stream()
                            .map(ci -> precioDeItem(ci.getItem()).multiply(BigDecimal.valueOf(ci.getCantidad())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CarritoGrupoResponse.builder()
                            .rescatistaId(rescatista.getId())
                            .rescatistaNombre(rescatista.getOrganizacion() != null
                                    ? rescatista.getOrganizacion()
                                    : rescatista.getNombre() + " " + rescatista.getApellido())
                            .items(itemsResponse)
                            .total(total)
                            .build();
                })
                .toList();
    }

    // items del carrito de un adoptante para un rescatista en particular (usado al pagar)
    @Transactional(readOnly = true)
    public List<CarritoItem> listarPorRescatista(User adoptante, Long rescatistaId) {
        return carritoItemRepository.findByAdoptante(adoptante).stream()
                .filter(ci -> ci.getItem().getRescatista().getId().equals(rescatistaId))
                .toList();
    }

    // saca del carrito los items que ya se pagaron
    @Transactional
    public void eliminarItemsDelCarrito(User adoptante, List<ItemTienda> items) {
        carritoItemRepository.deleteByAdoptanteAndItemIn(adoptante, items);
    }

    private CarritoItem getCarritoItemDelAdoptante(User adoptante, Long itemId) {
        ItemTienda item = itemTiendaRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        return carritoItemRepository.findByAdoptanteAndItem(adoptante, item)
                .orElseThrow(() -> new IllegalArgumentException("El ítem no está en tu carrito"));
    }

    private BigDecimal precioDeItem(ItemTienda item) {
        return item.getPrecio() != null ? item.getPrecio() : BigDecimal.ZERO;
    }

    private CarritoItemResponse toItemResponse(CarritoItem ci) {
        ItemTienda item = ci.getItem();
        String fotoUrl = item.getFotos().stream()
                .filter(f -> f.getEstado() == EstadoFoto.APROBADA)
                .map(f -> "/uploads/" + f.getNombreArchivo())
                .findFirst()
                .orElse(null);
        return CarritoItemResponse.builder()
                .itemId(item.getId())
                .titulo(item.getTitulo())
                .precio(item.getPrecio())
                .cantidad(ci.getCantidad())
                .stock(item.getStock())
                .fotoUrl(fotoUrl)
                .build();
    }
}
