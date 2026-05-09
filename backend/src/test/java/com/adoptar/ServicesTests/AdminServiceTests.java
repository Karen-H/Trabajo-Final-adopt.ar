package com.adoptar.ServicesTests;

import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoPendienteResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AdminServiceTests {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalFotoRepository animalFotoRepository;

    private User rescatista;
    private Animal animalPendiente;
    private AnimalFoto fotoPendiente;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        rescatista = User.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Garcia")
                .email("ana@test.com")
                .dni(12345678L)
                .tel("1122334455")
                .pass("hashedpass")
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .role(UserRole.USER)
                .activeProfile(UserProfile.RESCATISTA)
                .build();

        fotoPendiente = AnimalFoto.builder()
                .id(1L)
                .nombreArchivo("foto-uuid.jpg")
                .estado(EstadoFoto.PENDIENTE)
                .build();

        animalPendiente = Animal.builder()
                .id(1L)
                .nombre("Firulais")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .rescatista(rescatista)
                .aprobado(false)
                .rechazado(false)
                .fotos(new ArrayList<>(List.of(fotoPendiente)))
                .build();

        fotoPendiente.setAnimal(animalPendiente);
    }

    @Test
    public void testGetAnimalesPendientes_listaVacia() {
        when(animalRepository.findByAprobadoFalseAndRechazadoFalse()).thenReturn(List.of());

        List<AnimalResponse> resultado = adminService.getAnimalesPendientes();

        assertTrue(resultado.isEmpty());
    }

    @Test
    public void testGetAnimalesPendientes_retornaAnimales() {
        when(animalRepository.findByAprobadoFalseAndRechazadoFalse()).thenReturn(List.of(animalPendiente));

        List<AnimalResponse> resultado = adminService.getAnimalesPendientes();

        assertEquals(1, resultado.size());
        assertEquals("Firulais", resultado.get(0).getNombre());
        assertFalse(resultado.get(0).isAprobado());
        assertFalse(resultado.get(0).isRechazado());
    }

    @Test
    public void testAprobarAnimal_ok_aprueba_y_aprueba_fotos() {
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animalPendiente));
        when(animalRepository.save(any(Animal.class))).thenReturn(animalPendiente);

        AnimalResponse response = adminService.aprobarAnimal(1L);

        assertTrue(animalPendiente.isAprobado());
        assertFalse(animalPendiente.isRechazado());
        assertNull(animalPendiente.getMotivoRechazo());
        assertEquals(EstadoFoto.APROBADA, fotoPendiente.getEstado());
        assertTrue(response.isAprobado());
    }

    @Test
    public void testAprobarAnimal_yaAprobado_lanzaExcepcion() {
        animalPendiente.setAprobado(true);
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animalPendiente));

        assertThrows(IllegalArgumentException.class, () -> adminService.aprobarAnimal(1L));
    }

    @Test
    public void testRechazarAnimal_ok() {
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animalPendiente));
        when(animalRepository.save(any(Animal.class))).thenReturn(animalPendiente);

        AnimalResponse response = adminService.rechazarAnimal(1L, "Fotos inapropiadas");

        assertTrue(animalPendiente.isRechazado());
        assertEquals("Fotos inapropiadas", animalPendiente.getMotivoRechazo());
        assertTrue(response.isRechazado());
        assertEquals("Fotos inapropiadas", response.getMotivoRechazo());
    }

    @Test
    public void testRechazarAnimal_yaAprobado_lanzaExcepcion() {
        animalPendiente.setAprobado(true);
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animalPendiente));

        assertThrows(IllegalArgumentException.class, () -> adminService.rechazarAnimal(1L, "motivo"));
    }

    @Test
    public void testGetFotosPendientes_retornaFotos() {
        animalPendiente.setAprobado(true);
        when(animalFotoRepository.findByEstadoAndAnimal_Aprobado(EstadoFoto.PENDIENTE, true))
                .thenReturn(List.of(fotoPendiente));

        List<FotoPendienteResponse> resultado = adminService.getFotosPendientes();

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getAnimalId());
        assertEquals("Firulais", resultado.get(0).getAnimalNombre());
    }

    @Test
    public void testAprobarFoto_ok() {
        animalPendiente.setAprobado(true);
        when(animalFotoRepository.findById(1L)).thenReturn(Optional.of(fotoPendiente));
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenReturn(fotoPendiente);

        FotoResponse response = adminService.aprobarFoto(1L);

        assertEquals(EstadoFoto.APROBADA, fotoPendiente.getEstado());
        assertNull(fotoPendiente.getMotivoRechazo());
        assertEquals(EstadoFoto.APROBADA, response.getEstado());
    }

    @Test
    public void testRechazarFoto_ok() {
        animalPendiente.setAprobado(true);
        when(animalFotoRepository.findById(1L)).thenReturn(Optional.of(fotoPendiente));
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenReturn(fotoPendiente);

        FotoResponse response = adminService.rechazarFoto(1L, "Foto borrosa");

        assertEquals(EstadoFoto.RECHAZADA, fotoPendiente.getEstado());
        assertEquals("Foto borrosa", fotoPendiente.getMotivoRechazo());
        assertEquals(EstadoFoto.RECHAZADA, response.getEstado());
    }
}
