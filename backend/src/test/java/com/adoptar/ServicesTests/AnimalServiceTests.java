package com.adoptar.ServicesTests;

import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.service.AnimalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnimalServiceTests {

    @InjectMocks
    private AnimalService animalService;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalFotoRepository animalFotoRepository;

    private User rescatista;
    private AnimalRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(animalService, "uploadsPath", System.getProperty("java.io.tmpdir") + "/adoptar-test-uploads");

        rescatista = User.builder()
                .id(1L)
                .nombre("Laura")
                .apellido("López")
                .email("laura@adoptar.com")
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .role(UserRole.USER)
                .activeProfile(UserProfile.RESCATISTA)
                .build();

        request = new AnimalRequest();
        request.setNombre("Firulais");
        request.setSexo(SexoAnimal.MACHO);
        request.setEdad(RangoEdad.JOVEN);
        request.setTipo(TipoAnimal.PERRO);
        request.setTipoAdopcion(TipoAdopcion.PERMANENTE);
        request.setAmigableConGatos(true);
        request.setAmigableConPerros(false);
        request.setAmigableConNinos(true);
        request.setDescripcion("Un perro muy bueno");
    }

    @Test
    public void testCrearAnimal_sinUbicacion_lanzaExcepcion() {
        rescatista.setProvincia(null);
        List<MultipartFile> fotos = List.of(fotoValida());

        assertThrows(IllegalArgumentException.class, () -> animalService.crearAnimal(rescatista, request, fotos));
    }

    @Test
    public void testCrearAnimal_sinFotos_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> animalService.crearAnimal(rescatista, request, List.of()));
    }

    @Test
    public void testCrearAnimal_masDeCincoFotos_lanzaExcepcion() {
        List<MultipartFile> fotos = List.of(
                fotoValida(), fotoValida(), fotoValida(), fotoValida(), fotoValida(), fotoValida()
        );

        assertThrows(IllegalArgumentException.class, () -> animalService.crearAnimal(rescatista, request, fotos));
    }

    @Test
    public void testCrearAnimal_fotoConTipoInvalido_lanzaExcepcion() {
        MultipartFile fotoInvalida = new MockMultipartFile("fotos", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class, () -> animalService.crearAnimal(rescatista, request, List.of(fotoInvalida)));
    }

    @Test
    public void testCrearAnimal_ok_guardaAnimalYFoto() {
        Animal animalGuardado = Animal.builder()
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
                .fotos(new ArrayList<>())
                .build();

        when(animalRepository.save(any(Animal.class))).thenReturn(animalGuardado);
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = animalService.crearAnimal(rescatista, request, List.of(fotoValida()));

        assertNotNull(response);
        assertEquals("Firulais", response.getNombre());
        assertEquals(EstadoAnimal.EN_ADOPCION, response.getEstado());
        assertEquals("Buenos Aires", response.getProvincia());
        verify(animalRepository, times(1)).save(any(Animal.class));
    }

    @Test
    public void testGetMisAnimales_devuelveListaDelRescatista() {
        Animal animal = Animal.builder()
                .id(1L)
                .nombre("Michi")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.TRANSITO)
                .estado(EstadoAnimal.EN_ADOPCION)
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .rescatista(rescatista)
                .fotos(new ArrayList<>())
                .build();

        when(animalRepository.findByRescatista(rescatista)).thenReturn(List.of(animal));

        List<AnimalResponse> lista = animalService.getMisAnimales(rescatista);

        assertEquals(1, lista.size());
        assertEquals("Michi", lista.get(0).getNombre());
    }

    @Test
    public void testGetMisAnimales_listaVacia() {
        when(animalRepository.findByRescatista(rescatista)).thenReturn(List.of());

        List<AnimalResponse> lista = animalService.getMisAnimales(rescatista);

        assertTrue(lista.isEmpty());
    }

    @Test
    public void testCambiarEstado_ok() {
        Animal animal = Animal.builder()
                .id(1L)
                .nombre("Rex")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.SENIOR)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .rescatista(rescatista)
                .fotos(new ArrayList<>())
                .build();

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(animalRepository.save(any(Animal.class))).thenReturn(animal);

        AnimalResponse response = animalService.cambiarEstado(1L, rescatista, EstadoAnimal.ADOPTADO);

        assertEquals(EstadoAnimal.ADOPTADO, response.getEstado());
        verify(animalRepository, times(1)).save(animal);
    }

    @Test
    public void testCambiarEstado_animalNoEncontrado_lanzaExcepcion() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> animalService.cambiarEstado(99L, rescatista, EstadoAnimal.ADOPTADO));
    }

    @Test
    public void testCambiarEstado_rescatistaDistinto_lanzaExcepcion() {
        User otroRescatista = User.builder().id(2L).build();

        Animal animal = Animal.builder()
                .id(1L)
                .nombre("Rex")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .rescatista(otroRescatista)
                .fotos(new ArrayList<>())
                .build();

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));

        assertThrows(IllegalArgumentException.class, () -> animalService.cambiarEstado(1L, rescatista, EstadoAnimal.ADOPTADO));
    }

    private MockMultipartFile fotoValida() {
        return new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
