package com.adoptar.service;

import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.DashboardStatsResponse;
import com.adoptar.dto.response.EspecieStatsResponse;
import com.adoptar.dto.response.FotoPendienteResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.dto.response.ItemFotoPendienteResponse;
import com.adoptar.dto.response.ItemTiendaResponse;
import com.adoptar.dto.response.MesCountResponse;
import com.adoptar.dto.response.TiendaActivaResponse;
import com.adoptar.dto.response.UsuarioAdminResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.ItemFoto;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.EstadoItem;
import com.adoptar.enums.TipoAdopcion;
import com.adoptar.enums.TipoAnimal;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.DenunciaRepository;
import com.adoptar.repository.FavoritoRepository;
import com.adoptar.repository.ItemFotoRepository;
import com.adoptar.repository.ItemTiendaRepository;
import com.adoptar.repository.SolicitudTiendaRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;
    private final ItemTiendaRepository itemTiendaRepository;
    private final ItemFotoRepository itemFotoRepository;
    private final ItemTiendaService itemTiendaService;
    private final UserRepository userRepository;
    private final FavoritoRepository favoritoRepository;
    private final DenunciaRepository denunciaRepository;
    private final SolicitudTiendaRepository solicitudTiendaRepository;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalesPendientes() {
        List<Animal> pendientes = new ArrayList<>();
        pendientes.addAll(animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalseAndEliminadoFalse(CategoriaAnimal.ADOPCION));
        pendientes.addAll(animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalseAndEliminadoFalse(CategoriaAnimal.PERDIDO_ENCONTRADO));
        return pendientes.stream()
                .map(this::toAnimalResponse)
                .toList();
    }

    @Transactional
    public AnimalResponse aprobarAnimal(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isAprobado()) {
            throw new IllegalArgumentException("El animal ya está aprobado");
        }
        animal.setAprobado(true);
        animal.setRechazado(false);
        animal.setMotivoRechazo(null);
        // aprueba todas las fotos pendientes del animal
        animal.getFotos().forEach(f -> {
            if (f.getEstado() == EstadoFoto.PENDIENTE) {
                f.setEstado(EstadoFoto.APROBADA);
            }
        });
        animalRepository.save(animal);
        String nombreAnimal = animal.getNombre() != null ? animal.getNombre() : animal.getTipo().name();
        notificacionService.crear(animal.getPublicador(), TipoNotificacion.PUBLICACION_APROBADA,
                "Tu publicación \"" + nombreAnimal + "\" fue aprobada",
                "/mis-publicaciones");
        return toAnimalResponse(animal);
    }

    @Transactional
    public AnimalResponse rechazarAnimal(Long id, String motivo) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isAprobado()) {
            throw new IllegalArgumentException("El animal ya fue aprobado, no puede rechazarse");
        }
        animal.setRechazado(true);
        animal.setMotivoRechazo(motivo);
        animalRepository.save(animal);
        String nombreAnimal = animal.getNombre() != null ? animal.getNombre() : animal.getTipo().name();
        notificacionService.crear(animal.getPublicador(), TipoNotificacion.PUBLICACION_RECHAZADA,
                "Tu publicación \"" + nombreAnimal + "\" fue rechazada: " + motivo,
                "/mis-publicaciones");
        return toAnimalResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<FotoPendienteResponse> getFotosPendientes() {
        return animalFotoRepository.findByEstadoAndAnimal_Aprobado(EstadoFoto.PENDIENTE, true)
                .stream()
                .map(this::toFotoPendienteResponse)
                .toList();
    }

    @Transactional
    public FotoResponse aprobarFoto(Long id) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getAnimal().isAprobado()) {
            throw new IllegalArgumentException("Esta foto pertenece a un animal que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.APROBADA);
        foto.setMotivoRechazo(null);
        animalFotoRepository.save(foto);
        return toFotoResponse(foto);
    }

    @Transactional
    public FotoResponse rechazarFoto(Long id, String motivo) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getAnimal().isAprobado()) {
            throw new IllegalArgumentException("Esta foto pertenece a un animal que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.RECHAZADA);
        foto.setMotivoRechazo(motivo);
        animalFotoRepository.save(foto);
        return toFotoResponse(foto);
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getPublicaciones() {
        return animalRepository.findByAprobadoTrueAndEliminadoFalse()
                .stream()
                .map(this::toAnimalResponse)
                .toList();
    }

    @Transactional
    public void eliminarAnimal(Long id, String motivo) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isEliminado()) {
            throw new IllegalArgumentException("El animal ya fue eliminado");
        }
        animal.setEliminado(true);
        animal.setEliminadoPorAdmin(true);
        animal.setMotivoEliminacion(motivo);
        animalRepository.save(animal);
        notificacionService.crearParaFavoritosDeAnimal(
                id,
                TipoNotificacion.ANIMAL_FAVORITO_NO_DISPONIBLE,
                "Un animal en tus favoritos ya no está disponible",
                "/favoritos");
    }

    @Transactional
    public void eliminarFoto(Long id, String motivo) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        foto.setEstado(EstadoFoto.ELIMINADA);
        foto.setMotivoRechazo(motivo);
        animalFotoRepository.save(foto);
    }

    private AnimalResponse toAnimalResponse(Animal animal) {
        List<FotoResponse> fotos = animal.getFotos().stream()
                .map(f -> FotoResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .estado(f.getEstado())
                        .motivoRechazo(f.getMotivoRechazo())
                        .build())
                .toList();
        return AnimalResponse.builder()
                .id(animal.getId())
                .usuarioId(animal.getPublicador().getId())
                .categoria(animal.getCategoria())
                .nombre(animal.getNombre())
                .sexo(animal.getSexo())
                .edad(animal.getEdad())
                .tipo(animal.getTipo())
                .tipoAdopcion(animal.getTipoAdopcion())
                .estado(animal.getEstado())
                .amigableConGatos(animal.getAmigableConGatos())
                .amigableConPerros(animal.getAmigableConPerros())
                .amigableConNinos(animal.getAmigableConNinos())
                .descripcion(animal.getDescripcion())
                .direccion(animal.getDireccion())
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .provincia(animal.getPublicador().getProvincia())
                .ciudad(animal.getPublicador().getCiudad())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .eliminado(animal.isEliminado())
                .eliminadoPorAdmin(animal.isEliminadoPorAdmin())
                .motivoEliminacion(animal.getMotivoEliminacion())
                .creadoEn(animal.getCreadoEn())
                .adoptadoEn(animal.getAdoptadoEn())
                .build();
    }

    private FotoPendienteResponse toFotoPendienteResponse(AnimalFoto foto) {
        return FotoPendienteResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .animalId(foto.getAnimal().getId())
                .animalNombre(foto.getAnimal().getNombre())
                .animalTipo(foto.getAnimal().getTipo().name())
                .animalCategoria(foto.getAnimal().getCategoria().name())
                .animalEstado(foto.getAnimal().getEstado().name())
                .rescatistaNombre(foto.getAnimal().getPublicador().getNombre() + " " + foto.getAnimal().getPublicador().getApellido())
                .build();
    }

    private FotoResponse toFotoResponse(AnimalFoto foto) {
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .motivoRechazo(foto.getMotivoRechazo())
                .build();
    }

    // --- items de tienda ---

    @Transactional(readOnly = true)
    public List<ItemTiendaResponse> getItemsPendientes() {
        return itemTiendaRepository.findByEstadoAndEliminadoFalse(EstadoItem.PENDIENTE)
                .stream()
                .map(itemTiendaService::toResponse)
                .toList();
    }

    @Transactional
    public ItemTiendaResponse aprobarItem(Long id) {
        ItemTienda item = itemTiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (item.getEstado() == EstadoItem.APROBADO) {
            throw new IllegalArgumentException("El ítem ya está aprobado");
        }
        item.setEstado(EstadoItem.APROBADO);
        item.setMotivoRechazo(null);
        item.getFotos().forEach(f -> {
            if (f.getEstado() == EstadoFoto.PENDIENTE) {
                f.setEstado(EstadoFoto.APROBADA);
            }
        });
        itemTiendaRepository.save(item);
        return itemTiendaService.toResponse(item);
    }

    @Transactional
    public ItemTiendaResponse rechazarItem(Long id, String motivo) {
        ItemTienda item = itemTiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (item.getEstado() == EstadoItem.APROBADO) {
            throw new IllegalArgumentException("El ítem ya fue aprobado, no puede rechazarse");
        }
        item.setEstado(EstadoItem.RECHAZADO);
        item.setMotivoRechazo(motivo);
        itemTiendaRepository.save(item);
        return itemTiendaService.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemFotoPendienteResponse> getFotosItemPendientes() {
        return itemFotoRepository.findByEstadoAndItem_EstadoAndItem_EliminadoFalse(EstadoFoto.PENDIENTE, EstadoItem.APROBADO)
                .stream()
                .map(f -> ItemFotoPendienteResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .itemId(f.getItem().getId())
                        .itemTitulo(f.getItem().getTitulo())
                        .itemTipo(f.getItem().getTipo().name())
                        .rescatistaNombre(f.getItem().getRescatista().getNombre() + " " + f.getItem().getRescatista().getApellido())
                        .build())
                .toList();
    }

    @Transactional
    public FotoResponse aprobarFotoItem(Long id) {
        ItemFoto foto = itemFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (foto.getItem().getEstado() != EstadoItem.APROBADO) {
            throw new IllegalArgumentException("Esta foto pertenece a un ítem que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.APROBADA);
        foto.setMotivoRechazo(null);
        itemFotoRepository.save(foto);
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .build();
    }

    @Transactional
    public FotoResponse rechazarFotoItem(Long id, String motivo) {
        ItemFoto foto = itemFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (foto.getItem().getEstado() != EstadoItem.APROBADO) {
            throw new IllegalArgumentException("Esta foto pertenece a un ítem que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.RECHAZADA);
        foto.setMotivoRechazo(motivo);
        itemFotoRepository.save(foto);
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .motivoRechazo(motivo)
                .build();
    }

    // --- tiendas activas ---

    @Transactional(readOnly = true)
    public List<TiendaActivaResponse> listarTiendasActivas() {
        return userRepository.findByTieneTiendaTrue().stream()
                .map(u -> TiendaActivaResponse.builder()
                        .usuarioId(u.getId())
                        .nombre(u.getNombre())
                        .apellido(u.getApellido())
                        .email(u.getEmail())
                        .tel(u.getTel())
                        .organizacion(u.getOrganizacion())
                        .provincia(u.getProvincia())
                        .ciudad(u.getCiudad())
                        .build())
                .toList();
    }

    @Transactional
    public void revocarTienda(Long usuarioId) {
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!usuario.isTieneTienda()) {
            throw new IllegalArgumentException("El usuario no tiene tienda activa");
        }
        usuario.setTieneTienda(false);
        userRepository.save(usuario);
    }

    // --- usuarios ---

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listarUsuarios() {
        return userRepository.findAll().stream()
                .map(u -> UsuarioAdminResponse.builder()
                        .id(u.getId())
                        .nombre(u.getNombre())
                        .apellido(u.getApellido())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void eliminarUsuario(Long userId) {
        User usuario = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // denuncias que el usuario hizo sobre animales de otros
        denunciaRepository.deleteByDenuncianteId(userId);

        // animales del usuario y sus dependencias
        List<Animal> animales = animalRepository.findByPublicador(usuario);
        if (!animales.isEmpty()) {
            List<Long> animalIds = animales.stream().map(Animal::getId).toList();
            favoritoRepository.deleteByAnimalIdIn(animalIds);
            denunciaRepository.deleteByAnimalIdIn(animalIds);
            animalRepository.deleteAll(animales);
        }

        // favoritos del usuario sobre animales de otros
        favoritoRepository.deleteByUsuarioId(userId);

        // solicitud de tienda
        solicitudTiendaRepository.findByRescatistaId(userId)
                .ifPresent(solicitudTiendaRepository::delete);

        // items de tienda del usuario (fotos en cascada)
        List<ItemTienda> items = itemTiendaRepository.findAllByRescatista(usuario);
        itemTiendaRepository.deleteAll(items);

        userRepository.delete(usuario);
    }

    @Transactional
    public UsuarioAdminResponse actualizarRol(Long userId, UserRole nuevoRol) {
        User usuario = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("No se puede cambiar el rol de un administrador");
        }
        if (nuevoRol == UserRole.ADMIN) {
            throw new IllegalArgumentException("No se puede ascender a administrador");
        }
        if (usuario.getRole() == nuevoRol) {
            throw new IllegalArgumentException("El usuario ya tiene ese rol");
        }
        usuario.setRole(nuevoRol);
        userRepository.save(usuario);
        return UsuarioAdminResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .createdAt(usuario.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime desde = LocalDateTime.now().minusMonths(12);

        // adopciones
        long totalAdoptados = animalRepository.countByCategoriaAndEstado(CategoriaAnimal.ADOPCION, EstadoAnimal.ADOPTADO);
        long enAdopcionActivos = animalRepository.countEnAdopcionActivos();
        long transitoActivos = animalRepository.countActivosByTipoAdopcion(TipoAdopcion.TRANSITO);
        long permanenteActivos = animalRepository.countActivosByTipoAdopcion(TipoAdopcion.PERMANENTE);

        // reportes activos
        long perdidosActivos = animalRepository.countPerdidosActivos();
        long encontradosActivos = animalRepository.countEncontradosActivos();

        // adoptados por mes
        List<Object[]> rawAdoptados = animalRepository.countAdoptadosPorMes(desde);
        List<MesCountResponse> adoptadosPorMes = rawAdoptados.stream()
                .map(row -> MesCountResponse.builder()
                        .mes(String.format("%04d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                        .cantidad(((Number) row[2]).longValue())
                        .build())
                .toList();

        // usuarios
        long totalUsuarios = userRepository.count();
        List<Object[]> rawUsuarios = userRepository.countUsuariosPorMes(desde);
        List<MesCountResponse> usuariosPorMes = rawUsuarios.stream()
                .map(row -> MesCountResponse.builder()
                        .mes(String.format("%04d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                        .cantidad(((Number) row[2]).longValue())
                        .build())
                .toList();

        // por especie
        Map<String, Long> totalPorEspecie = animalRepository.countByTipoHistorico().stream()
                .collect(Collectors.toMap(r -> ((TipoAnimal) r[0]).name(), r -> ((Number) r[1]).longValue()));
        Map<String, Long> publicadosPorEspecie = animalRepository.countPublicadosByTipo().stream()
                .collect(Collectors.toMap(r -> ((TipoAnimal) r[0]).name(), r -> ((Number) r[1]).longValue()));
        List<EspecieStatsResponse> animalPorEspecie = Arrays.stream(TipoAnimal.values())
                .map(t -> EspecieStatsResponse.builder()
                        .especie(t.name())
                        .totalHistorico(totalPorEspecie.getOrDefault(t.name(), 0L))
                        .publicadosActuales(publicadosPorEspecie.getOrDefault(t.name(), 0L))
                        .build())
                .toList();

        // tasa de éxito
        long totalHistoricoAdopcion = animalRepository.countTotalHistoricoAdopcion();
        long totalHistoricoPerdidos = animalRepository.countTotalHistoricoPerdidos();
        long totalHistoricoEncontrados = animalRepository.countTotalHistoricoEncontrados();
        long resueltosPerdidos = animalRepository.countResueltosPerdidos();
        long resueltosEncontrados = animalRepository.countResueltosEncontrados();
        long totalEliminados = animalRepository.countEliminados();

        return DashboardStatsResponse.builder()
                .totalAdoptados(totalAdoptados)
                .adoptadosPorMes(adoptadosPorMes)
                .enAdopcionActivos(enAdopcionActivos)
                .transitoActivos(transitoActivos)
                .permanenteActivos(permanenteActivos)
                .perdidosActivos(perdidosActivos)
                .encontradosActivos(encontradosActivos)
                .totalUsuarios(totalUsuarios)
                .usuariosPorMes(usuariosPorMes)
                .animalPorEspecie(animalPorEspecie)
                .totalHistoricoAdopcion(totalHistoricoAdopcion)
                .totalHistoricoPerdidos(totalHistoricoPerdidos)
                .totalHistoricoEncontrados(totalHistoricoEncontrados)
                .resueltosPerdidos(resueltosPerdidos)
                .resueltosEncontrados(resueltosEncontrados)
                .totalEliminados(totalEliminados)
                .build();
    }
}
