package com.adoptar.service;

import com.adoptar.dto.request.ItemTiendaRequest;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.dto.response.ItemTiendaResponse;
import com.adoptar.dto.response.RescatistaTiendaResponse;
import com.adoptar.entity.ItemFoto;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.EstadoItem;
import com.adoptar.repository.ItemFotoRepository;
import com.adoptar.repository.ItemTiendaRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemTiendaService {

    private final ItemTiendaRepository itemTiendaRepository;
    private final ItemFotoRepository itemFotoRepository;
    private final UserRepository userRepository;

    @Value("${uploads.path}")
    private String uploadsPath;

    private static final Set<String> TIPOS_IMAGEN = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Transactional
    public ItemTiendaResponse crear(User rescatista, ItemTiendaRequest request, List<MultipartFile> fotos) {
        if (!rescatista.isTieneTienda()) {
            throw new IllegalArgumentException("No tenés una tienda habilitada");
        }
        validarFotos(fotos);

        ItemTienda item = ItemTienda.builder()
                .rescatista(rescatista)
                .titulo(request.getTitulo())
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .build();

        itemTiendaRepository.save(item);
        guardarFotos(item, fotos);
        return toResponse(item);
    }

    @Transactional
    public ItemTiendaResponse editar(Long itemId, User rescatista, ItemTiendaRequest request) {
        ItemTienda item = getItemDelRescatista(itemId, rescatista);
        item.setTitulo(request.getTitulo());
        item.setTipo(request.getTipo());
        item.setDescripcion(request.getDescripcion());
        item.setPrecio(request.getPrecio());
        item.setStock(request.getStock());
        itemTiendaRepository.save(item);
        return toResponse(item);
    }

    @Transactional
    public ItemTiendaResponse agregarFotos(Long itemId, User rescatista, List<MultipartFile> fotosNuevas) {
        ItemTienda item = getItemDelRescatista(itemId, rescatista);
        if (fotosNuevas == null || fotosNuevas.isEmpty()) {
            throw new IllegalArgumentException("Debes subir al menos una foto");
        }
        long fotosActivas = item.getFotos().stream()
                .filter(f -> f.getEstado() != EstadoFoto.ELIMINADA)
                .count();
        if (fotosActivas + fotosNuevas.size() > 5) {
            throw new IllegalArgumentException("No podés tener más de 5 fotos por ítem");
        }
        for (MultipartFile foto : fotosNuevas) {
            String contentType = foto.getContentType();
            if (contentType == null || !TIPOS_IMAGEN.contains(contentType)) {
                throw new IllegalArgumentException("Solo se aceptan imágenes (jpg, png, webp, gif)");
            }
        }
        guardarFotos(item, fotosNuevas);
        return toResponse(item);
    }

    @Transactional
    public ItemTiendaResponse eliminarFoto(Long itemId, Long fotoId, User rescatista) {
        ItemTienda item = getItemDelRescatista(itemId, rescatista);
        ItemFoto foto = item.getFotos().stream()
                .filter(f -> f.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        long fotasActivas = item.getFotos().stream()
                .filter(f -> f.getEstado() != EstadoFoto.ELIMINADA && !f.getId().equals(fotoId))
                .count();
        if (fotasActivas < 1) {
            throw new IllegalArgumentException("El ítem debe tener al menos una foto");
        }
        foto.setEstado(EstadoFoto.ELIMINADA);
        itemFotoRepository.save(foto);
        return toResponse(item);
    }

    @Transactional
    public void eliminar(Long itemId, User rescatista) {
        ItemTienda item = getItemDelRescatista(itemId, rescatista);
        item.setEliminado(true);
        itemTiendaRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<ItemTiendaResponse> getMisItems(User rescatista) {
        return itemTiendaRepository.findByRescatistaAndEliminadoFalse(rescatista)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // catalogo publico: rescatistas con tienda habilitada

    @Transactional(readOnly = true)
    public List<RescatistaTiendaResponse> listarTiendas(String provincia, String q, User usuarioActual) {
        return userRepository.findByTieneTiendaTrue().stream()
                .filter(u -> usuarioActual == null || !u.getId().equals(usuarioActual.getId()))
                .filter(u -> provincia == null || provincia.isBlank()
                        || provincia.equalsIgnoreCase(u.getProvincia()))
                .filter(u -> {
                    if (q == null || q.isBlank()) return true;
                    String busqueda = q.toLowerCase();
                    boolean enOrg = u.getOrganizacion() != null
                            && u.getOrganizacion().toLowerCase().contains(busqueda);
                    boolean enNombre = (u.getNombre() + " " + u.getApellido())
                            .toLowerCase().contains(busqueda);
                    return enOrg || enNombre;
                })
                .map(u -> RescatistaTiendaResponse.builder()
                        .id(u.getId())
                        .nombre(u.getNombre())
                        .apellido(u.getApellido())
                        .organizacion(u.getOrganizacion())
                        .provincia(u.getProvincia())
                        .ciudad(u.getCiudad())
                        .build())
                .toList();
    }

    // catalogo publico: items aprobados de una tienda

    @Transactional(readOnly = true)
    public List<ItemTiendaResponse> listarItemsDeTienda(Long rescatistaId) {
        User rescatista = userRepository.findById(rescatistaId)
                .filter(User::isTieneTienda)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada"));
        return itemTiendaRepository.findByRescatistaAndEliminadoFalse(rescatista).stream()
                .filter(i -> i.getEstado() == EstadoItem.APROBADO)
                .map(this::toResponse)
                .toList();
    }

    private ItemTienda getItemDelRescatista(Long itemId, User rescatista) {
        ItemTienda item = itemTiendaRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (!item.getRescatista().getId().equals(rescatista.getId())) {
            throw new IllegalArgumentException("No tenés permiso para modificar este ítem");
        }
        if (item.isEliminado()) {
            throw new IllegalArgumentException("El ítem fue eliminado");
        }
        return item;
    }

    private void validarFotos(List<MultipartFile> fotos) {
        if (fotos == null || fotos.isEmpty()) {
            throw new IllegalArgumentException("Debes subir al menos una foto");
        }
        if (fotos.size() > 5) {
            throw new IllegalArgumentException("No podés subir más de 5 fotos");
        }
        for (MultipartFile foto : fotos) {
            String contentType = foto.getContentType();
            if (contentType == null || !TIPOS_IMAGEN.contains(contentType)) {
                throw new IllegalArgumentException("Solo se aceptan imágenes (jpg, png, webp, gif)");
            }
        }
    }

    private void guardarFotos(ItemTienda item, List<MultipartFile> fotos) {
        for (MultipartFile foto : fotos) {
            String extension = getExtension(foto.getOriginalFilename());
            String nombreArchivo = UUID.randomUUID() + extension;
            try {
                foto.transferTo(Paths.get(uploadsPath).resolve(nombreArchivo));
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar la foto", e);
            }
            ItemFoto itemFoto = ItemFoto.builder()
                    .item(item)
                    .nombreArchivo(nombreArchivo)
                    .build();
            itemFotoRepository.save(itemFoto);
            item.getFotos().add(itemFoto);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    public ItemTiendaResponse toResponse(ItemTienda item) {
        List<FotoResponse> fotos = item.getFotos().stream()
                .map(f -> FotoResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .estado(f.getEstado())
                        .motivoRechazo(f.getMotivoRechazo())
                        .build())
                .toList();
        return ItemTiendaResponse.builder()
                .id(item.getId())
                .titulo(item.getTitulo())
                .tipo(item.getTipo())
                .descripcion(item.getDescripcion())
                .precio(item.getPrecio())
                .stock(item.getStock())
                .fotos(fotos)
                .estado(item.getEstado())
                .motivoRechazo(item.getMotivoRechazo())
                .rescatistaNombre(item.getRescatista().getNombre() + " " + item.getRescatista().getApellido())
                .creadoEn(item.getCreadoEn())
                .build();
    }
}
