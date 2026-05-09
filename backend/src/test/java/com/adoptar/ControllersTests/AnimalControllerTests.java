package com.adoptar.ControllersTests;

import com.adoptar.controller.AnimalController;
import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.service.AnimalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AnimalControllerTests {

    @InjectMocks
    private AnimalController animalController;

    @Mock
    private AnimalService animalService;

    private User rescatista;
    private User adoptante;
    private AnimalRequest request;
    private AnimalResponse mockResponse;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

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

        adoptante = User.builder()
                .id(2L)
                .nombre("Carlos")
                .apellido("Pérez")
                .email("carlos@adoptar.com")
                .role(UserRole.USER)
                .activeProfile(UserProfile.ADOPTANTE)
                .build();

        request = new AnimalRequest();
        request.setNombre("Firulais");
        request.setSexo(SexoAnimal.MACHO);
        request.setEdad(RangoEdad.JOVEN);
        request.setTipo(TipoAnimal.PERRO);
        request.setTipoAdopcion(TipoAdopcion.PERMANENTE);

        mockResponse = AnimalResponse.builder()
                .id(1L)
                .nombre("Firulais")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .provincia("Buenos Aires")
                .ciudad("La Plata")
                .rescatistaNombre("Laura López")
                .fotos(List.of())
                .build();
    }

    @Test
    public void testCrear_comoRescatista_devuelveCreated() {
        List<MultipartFile> fotos = List.of(new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1}));
        when(animalService.crearAnimal(any(User.class), any(AnimalRequest.class), any())).thenReturn(mockResponse);

        ResponseEntity<?> response = animalController.crear(rescatista, request, fotos);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    public void testCrear_comoAdoptante_devuelveForbidden() {
        List<MultipartFile> fotos = List.of(new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1}));

        ResponseEntity<?> response = animalController.crear(adoptante, request, fotos);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testCrear_errorDeValidacion_devuelveBadRequest() {
        List<MultipartFile> fotos = List.of(new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1}));
        when(animalService.crearAnimal(any(), any(), any())).thenThrow(new IllegalArgumentException("Debés subir al menos una foto"));

        ResponseEntity<?> response = animalController.crear(rescatista, request, fotos);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testGetMisAnimales_comoRescatista_devuelveOk() {
        when(animalService.getMisAnimales(rescatista)).thenReturn(List.of(mockResponse));

        ResponseEntity<List<AnimalResponse>> response = animalController.getMisAnimales(rescatista);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testGetMisAnimales_comoAdoptante_devuelveForbidden() {
        ResponseEntity<List<AnimalResponse>> response = animalController.getMisAnimales(adoptante);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testCambiarEstado_comoRescatista_devuelveOk() {
        AnimalResponse actualizado = AnimalResponse.builder()
                .id(1L).nombre("Firulais").sexo(SexoAnimal.MACHO).edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.PERRO).tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.ADOPTADO).provincia("Buenos Aires").ciudad("La Plata")
                .rescatistaNombre("Laura López").fotos(List.of()).build();

        when(animalService.cambiarEstado(eq(1L), eq(rescatista), eq(EstadoAnimal.ADOPTADO))).thenReturn(actualizado);

        ResponseEntity<?> response = animalController.cambiarEstado(rescatista, 1L, EstadoAnimal.ADOPTADO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testCambiarEstado_comoAdoptante_devuelveForbidden() {
        ResponseEntity<?> response = animalController.cambiarEstado(adoptante, 1L, EstadoAnimal.ADOPTADO);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testCambiarEstado_animalNoPertenece_devuelveBadRequest() {
        when(animalService.cambiarEstado(eq(1L), eq(rescatista), eq(EstadoAnimal.ADOPTADO)))
                .thenThrow(new IllegalArgumentException("No tenés permiso para modificar este animal"));

        ResponseEntity<?> response = animalController.cambiarEstado(rescatista, 1L, EstadoAnimal.ADOPTADO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
