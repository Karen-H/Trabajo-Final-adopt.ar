package com.adoptar.ServicesTests;

import com.adoptar.dto.request.ReporteRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReporteServiceTests {

    @InjectMocks
    private ReporteService reporteService;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalFotoRepository animalFotoRepository;

    private User publicador;
    private User admin;
    private ReporteRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(reporteService, "uploadsPath", System.getProperty("java.io.tmpdir") + "/adoptar-test-uploads");

        publicador = User.builder()
                .id(1L)
                .nombre("Juan")
                .apellido("Perez")
                .email("juan@test.com")
                .provincia("Cordoba")
                .ciudad("Cordoba")
                .role(UserRole.USER)
                .activeProfile(UserProfile.RESCATISTA)
                .build();

        admin = User.builder()
                .id(2L)
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@adoptar.com")
                .provincia("Buenos Aires")
                .ciudad("CABA")
                .role(UserRole.ADMIN)
                .build();

        request = new ReporteRequest();
        request.setTipo(TipoAnimal.PERRO);
        request.setEstadoInicial(EstadoAnimal.PERDIDO);
        request.setDireccion("Av. Colon 1234, Cordoba");
        request.setEnPosesionDelPublicador(false);
        request.setDescripcion("Perro chico, color marron");
    }

    @Test
    public void testCrearReporte_perdido_ok() throws IOException {
        Animal guardado = animalPerdido(publicador);
        MultipartFile foto = fotoMock();

        when(animalRepository.save(any(Animal.class))).thenReturn(guardado);
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = reporteService.crearReporte(publicador, request, List.of(foto));

        assertNotNull(response);
        assertEquals(EstadoAnimal.PERDIDO, response.getEstado());
        assertEquals(CategoriaAnimal.PERDIDO_ENCONTRADO, response.getCategoria());
        assertFalse(response.isAprobado());
        verify(animalRepository, times(1)).save(any(Animal.class));
    }

    @Test
    public void testCrearReporte_encontrado_ok() throws IOException {
        request.setEstadoInicial(EstadoAnimal.ENCONTRADO);
        Animal guardado = animalPerdido(publicador);
        guardado.setEstado(EstadoAnimal.ENCONTRADO);
        MultipartFile foto = fotoMock();

        when(animalRepository.save(any(Animal.class))).thenReturn(guardado);
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = reporteService.crearReporte(publicador, request, List.of(foto));

        assertNotNull(response);
        assertEquals(EstadoAnimal.ENCONTRADO, response.getEstado());
    }

    @Test
    public void testCrearReporte_estadoInvalido_lanzaExcepcion() {
        request.setEstadoInicial(EstadoAnimal.RESUELTO);

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.crearReporte(publicador, request, List.of(fotoValida())));
    }

    @Test
    public void testCrearReporte_sinFotos_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> reporteService.crearReporte(publicador, request, List.of()));
    }

    @Test
    public void testCrearReporte_masDeCincoFotos_lanzaExcepcion() {
        List<MultipartFile> fotos = List.of(
                fotoValida(), fotoValida(), fotoValida(), fotoValida(), fotoValida(), fotoValida()
        );

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.crearReporte(publicador, request, fotos));
    }

    @Test
    public void testCrearReporte_fotoInvalida_lanzaExcepcion() {
        MultipartFile pdf = new MockMultipartFile("fotos", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.crearReporte(publicador, request, List.of(pdf)));
    }

    @Test
    public void testCrearReporte_admin_aprueba_automaticamente() throws IOException {
        Animal guardado = animalPerdido(admin);
        guardado.setAprobado(true);
        MultipartFile foto = fotoMock();

        when(animalRepository.save(any(Animal.class))).thenReturn(guardado);
        when(animalFotoRepository.save(any(AnimalFoto.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = reporteService.crearReporte(admin, request, List.of(foto));

        assertTrue(response.isAprobado());
    }

    @Test
    public void testResolver_ok() {
        Animal reporte = animalPerdido(publicador);
        reporte.setAprobado(true);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(reporte));
        when(animalRepository.save(any(Animal.class))).thenReturn(reporte);

        AnimalResponse response = reporteService.resolver(1L, publicador);

        assertEquals(EstadoAnimal.RESUELTO, response.getEstado());
        verify(animalRepository, times(1)).save(reporte);
    }

    @Test
    public void testResolver_noEncontrado_lanzaExcepcion() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.resolver(99L, publicador));
    }

    @Test
    public void testResolver_otroPublicador_lanzaExcepcion() {
        Animal reporte = animalPerdido(publicador);
        User otro = User.builder().id(99L).build();

        when(animalRepository.findById(1L)).thenReturn(Optional.of(reporte));

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.resolver(1L, otro));
    }

    @Test
    public void testResolver_yaResuelto_lanzaExcepcion() {
        Animal reporte = animalPerdido(publicador);
        reporte.setEstado(EstadoAnimal.RESUELTO);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(reporte));

        assertThrows(IllegalArgumentException.class,
                () -> reporteService.resolver(1L, publicador));
    }

    @Test
    public void testGetMisReportes_devuelveSoloPerdidoEncontrado() {
        Animal reporte = animalPerdido(publicador);
        Animal adopcion = Animal.builder()
                .id(2L)
                .categoria(CategoriaAnimal.ADOPCION)
                .tipo(TipoAnimal.GATO)
                .estado(EstadoAnimal.EN_ADOPCION)
                .publicador(publicador)
                .fotos(new ArrayList<>())
                .build();

        when(animalRepository.findByPublicador(publicador)).thenReturn(List.of(reporte, adopcion));

        List<AnimalResponse> resultado = reporteService.getMisReportes(publicador);

        assertEquals(1, resultado.size());
        assertEquals(CategoriaAnimal.PERDIDO_ENCONTRADO, resultado.get(0).getCategoria());
    }

    @Test
    public void testGetPerdidos_devuelveAprobadosConEstadoPerdido() {
        Animal reporte = animalPerdido(publicador);
        reporte.setAprobado(true);

        when(animalRepository.findByCategoriaAndAprobadoTrueAndEstadoAndEliminadoFalse(
                CategoriaAnimal.PERDIDO_ENCONTRADO, EstadoAnimal.PERDIDO))
                .thenReturn(List.of(reporte));

        List<AnimalResponse> resultado = reporteService.getPerdidos();

        assertEquals(1, resultado.size());
        assertEquals(EstadoAnimal.PERDIDO, resultado.get(0).getEstado());
    }

    @Test
    public void testGetEncontrados_devuelveAprobadosConEstadoEncontrado() {
        Animal reporte = animalPerdido(publicador);
        reporte.setEstado(EstadoAnimal.ENCONTRADO);
        reporte.setAprobado(true);

        when(animalRepository.findByCategoriaAndAprobadoTrueAndEstadoAndEliminadoFalse(
                CategoriaAnimal.PERDIDO_ENCONTRADO, EstadoAnimal.ENCONTRADO))
                .thenReturn(List.of(reporte));

        List<AnimalResponse> resultado = reporteService.getEncontrados();

        assertEquals(1, resultado.size());
        assertEquals(EstadoAnimal.ENCONTRADO, resultado.get(0).getEstado());
    }

    // helpers

    private Animal animalPerdido(User pub) {
        return Animal.builder()
                .id(1L)
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .estado(EstadoAnimal.PERDIDO)
                .direccion("Av. Colon 1234")
                .enPosesionDelPublicador(false)
                .descripcion("Perro marron")
                .publicador(pub)
                .aprobado(false)
                .rechazado(false)
                .fotos(new ArrayList<>())
                .build();
    }

    private MockMultipartFile fotoValida() {
        return new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private MultipartFile fotoMock() throws IOException {
        MultipartFile foto = mock(MultipartFile.class);
        when(foto.getContentType()).thenReturn("image/jpeg");
        when(foto.getOriginalFilename()).thenReturn("foto.jpg");
        doNothing().when(foto).transferTo(any(java.nio.file.Path.class));
        return foto;
    }
}
