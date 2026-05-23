package com.adoptar.config;

import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialConfiguration implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${uploads.path}")
    private String uploadsPath;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Cargando datos iniciales...");
            copiarFotoSeed();
            seedUsers();
            User ingrid = userRepository.findByEmail("ingrid@adoptar.com").orElseThrow();
            User karen = userRepository.findByEmail("karen@adoptar.com").orElseThrow();
            seedAnimales(ingrid, karen);
            log.info("Datos iniciales cargados correctamente");
        } else {
            log.info("BD ya inicializada");
        }
        seedModeradores();
    }

    private void copiarFotoSeed() {
        try {
            Path destDir = Paths.get(uploadsPath);
            Files.createDirectories(destDir);
            Path dest = destDir.resolve("seed_foto.jpg");
            if (!Files.exists(dest)) {
                ClassPathResource resource = new ClassPathResource("seed/foto_seed.jpg");
                Files.copy(resource.getInputStream(), dest);
            }
        } catch (Exception e) {
            log.warn("No se pudo copiar la foto seed: {}", e.getMessage());
        }
    }

    private void seedUsers() {
        userRepository.save(User.builder()
                .nombre("Admin")
                .apellido("Sistema")
                .dni(1000001L)
                .email("admin@adoptar.com")
                .tel("1100000001")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build());

        userRepository.save(User.builder()
                .nombre("Ingrid")
                .apellido("Usuario")
                .dni(1000002L)
                .email("ingrid@adoptar.com")
                .tel("1100000002")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.USER)
                .build());

        userRepository.save(User.builder()
                .nombre("Karen")
                .apellido("Usuario")
                .dni(1000003L)
                .email("karen@adoptar.com")
                .tel("1100000003")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.USER)
                .build());

        log.info("Usuarios iniciales creados: admin@adoptar.com, ingrid@adoptar.com, karen@adoptar.com / admin123");
    }

    private void seedModeradores() {
        if (!userRepository.existsByEmail("mod1@adoptar.com")) {
            userRepository.save(User.builder()
                    .nombre("Moderador")
                    .apellido("Uno")
                    .dni(1000004L)
                    .email("mod1@adoptar.com")
                    .tel("1100000004")
                    .pass(passwordEncoder.encode("mod123"))
                    .role(UserRole.MODERADOR)
                    .build());
        }
        if (!userRepository.existsByEmail("mod2@adoptar.com")) {
            userRepository.save(User.builder()
                    .nombre("Moderador")
                    .apellido("Dos")
                    .dni(1000005L)
                    .email("mod2@adoptar.com")
                    .tel("1100000005")
                    .pass(passwordEncoder.encode("mod123"))
                    .role(UserRole.MODERADOR)
                    .build());
        }
        log.info("Moderadores listos: mod1@adoptar.com, mod2@adoptar.com / mod123");
    }

    private void seedAnimales(User ingrid, User karen) {

        // --- Ingrid: en revisión (ADOPCION, aprobado=false) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Max")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(false)
                .amigableConPerros(true)
                .amigableConNinos(true)
                .descripcion("Perro tranquilo que busca un hogar para siempre. Está vacunado y desparasitado.")
                .aprobado(false)
                .publicador(ingrid)
                .build(), EstadoFoto.PENDIENTE);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Luna")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.TRANSITO)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(true)
                .amigableConPerros(false)
                .amigableConNinos(true)
                .descripcion("Gatita muy cariñosa y sociable. Ideal para tránsito con opción a adopción definitiva.")
                .aprobado(false)
                .publicador(ingrid)
                .build(), EstadoFoto.PENDIENTE);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Rocky")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.CACHORRO)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(false)
                .amigableConPerros(true)
                .amigableConNinos(true)
                .descripcion("Cachorro juguetón lleno de energía. Necesita espacio para correr.")
                .aprobado(false)
                .publicador(ingrid)
                .build(), EstadoFoto.PENDIENTE);

        // --- Ingrid: en adopción (ADOPCION, aprobado=true, EN_ADOPCION) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Mia")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.TRANSITO)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(true)
                .amigableConPerros(false)
                .amigableConNinos(true)
                .descripcion("Gata dulce y tranquila, perfecta para tránsito.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Thor")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(false)
                .amigableConPerros(true)
                .amigableConNinos(true)
                .descripcion("Perro activo y leal, necesita adopción permanente.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Bella")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.SENIOR)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(false)
                .amigableConPerros(false)
                .amigableConNinos(false)
                .descripcion("Gatita mayor que busca un hogar tranquilo para sus últimos años.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        // --- Ingrid: adoptados (ADOPCION, aprobado=true, ADOPTADO) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Leo")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.ADOPTADO)
                .amigableConGatos(true)
                .amigableConPerros(false)
                .amigableConNinos(true)
                .descripcion("Gato independiente que ya encontró su hogar.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Nina")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.ADOPTADO)
                .amigableConGatos(true)
                .amigableConPerros(true)
                .amigableConNinos(true)
                .descripcion("Perra muy activa y sociable, ya fue adoptada con éxito.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Simba")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.CACHORRO)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.ADOPTADO)
                .amigableConGatos(false)
                .amigableConPerros(true)
                .amigableConNinos(true)
                .descripcion("Cachorro adorable que ya fue adoptado.")
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        // --- Ingrid: perdidos (PERDIDO_ENCONTRADO, aprobado=true, PERDIDO) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Se perdió cerca del parque. Tiene collar azul y responde al nombre de Beto.")
                .direccion("Av. Corrientes 3000")
                .latitud(-34.6037)
                .longitud(-58.3816)
                .provincia("Buenos Aires")
                .ciudad("Ciudad Autónoma de Buenos Aires")
                .fechaAvistamiento(LocalDate.of(2026, 5, 10))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.GATO)
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.JOVEN)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Gatita gris con mancha blanca en la pata izquierda. Se escapó del departamento.")
                .direccion("Callao 1500")
                .latitud(-34.5983)
                .longitud(-58.3932)
                .provincia("Buenos Aires")
                .ciudad("Ciudad Autónoma de Buenos Aires")
                .fechaAvistamiento(LocalDate.of(2026, 5, 15))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.ADULTO)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Perra mediana, color marrón, sin collar. Muy asustadiza.")
                .direccion("Bv. Oroño 400")
                .latitud(-32.9442)
                .longitud(-60.6505)
                .provincia("Santa Fe")
                .ciudad("Rosario")
                .fechaAvistamiento(LocalDate.of(2026, 5, 18))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        // --- Ingrid: eliminados (ADOPCION, eliminado=true) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Boby")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .descripcion("Publicación eliminada.")
                .aprobado(true)
                .eliminado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Tita")
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.JOVEN)
                .tipo(TipoAnimal.GATO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .descripcion("Publicación eliminada.")
                .aprobado(true)
                .eliminado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre("Roco")
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.SENIOR)
                .tipo(TipoAnimal.PERRO)
                .tipoAdopcion(TipoAdopcion.PERMANENTE)
                .estado(EstadoAnimal.EN_ADOPCION)
                .descripcion("Publicación eliminada.")
                .aprobado(false)
                .eliminado(true)
                .publicador(ingrid)
                .build(), EstadoFoto.PENDIENTE);

        // --- Karen: perdidos (PERDIDO_ENCONTRADO, aprobado=true, PERDIDO) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Perro grande, pelaje negro, collar rojo. Se perdió en el barrio.")
                .direccion("Av. Santa Fe 2100")
                .latitud(-34.5975)
                .longitud(-58.4008)
                .provincia("Buenos Aires")
                .ciudad("Ciudad Autónoma de Buenos Aires")
                .fechaAvistamiento(LocalDate.of(2026, 5, 20))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.GATO)
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.JOVEN)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Gata atigrada, ojos verdes. Se perdió en el centro.")
                .direccion("Gral. Paz 100")
                .latitud(-31.4201)
                .longitud(-64.1888)
                .provincia("Córdoba")
                .ciudad("Córdoba")
                .fechaAvistamiento(LocalDate.of(2026, 5, 19))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.OTRO)
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .estado(EstadoAnimal.PERDIDO)
                .descripcion("Conejo blanco con manchas marrones. Se escapó del jardín.")
                .direccion("Mitre 750")
                .latitud(-32.8908)
                .longitud(-68.8272)
                .provincia("Mendoza")
                .ciudad("Mendoza")
                .fechaAvistamiento(LocalDate.of(2026, 5, 21))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        // --- Karen: encontrados (PERDIDO_ENCONTRADO, aprobado=true, ENCONTRADO) ---

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .sexo(SexoAnimal.HEMBRA)
                .edad(RangoEdad.CACHORRO)
                .estado(EstadoAnimal.ENCONTRADO)
                .descripcion("Cachorrita encontrada en la calle, sin collar. Está en buen estado de salud.")
                .direccion("Av. Rivadavia 5000")
                .latitud(-34.6284)
                .longitud(-58.4380)
                .provincia("Buenos Aires")
                .ciudad("Ciudad Autónoma de Buenos Aires")
                .fechaAvistamiento(LocalDate.of(2026, 5, 22))
                .enPosesionDelPublicador(true)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.GATO)
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.ADULTO)
                .estado(EstadoAnimal.ENCONTRADO)
                .descripcion("Gato naranja encontrado en el patio de un local. Parece de raza.")
                .direccion("San Martín 200")
                .latitud(-31.4165)
                .longitud(-64.1836)
                .provincia("Córdoba")
                .ciudad("Córdoba")
                .fechaAvistamiento(LocalDate.of(2026, 5, 21))
                .enPosesionDelPublicador(false)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        guardar(Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(TipoAnimal.PERRO)
                .sexo(SexoAnimal.MACHO)
                .edad(RangoEdad.JOVEN)
                .estado(EstadoAnimal.ENCONTRADO)
                .descripcion("Perro mestizo encontrado deambulando por el barrio. Muy tranquilo y sociable.")
                .direccion("Pellegrini 900")
                .latitud(-32.9531)
                .longitud(-60.6582)
                .provincia("Santa Fe")
                .ciudad("Rosario")
                .fechaAvistamiento(LocalDate.of(2026, 5, 22))
                .enPosesionDelPublicador(true)
                .aprobado(true)
                .publicador(karen)
                .build(), EstadoFoto.APROBADA);

        log.info("Animales seed creados: 15 de Ingrid, 6 de Karen");
    }

    private void guardar(Animal animal, EstadoFoto estadoFoto) {
        AnimalFoto foto = AnimalFoto.builder()
                .animal(animal)
                .nombreArchivo("seed_foto.jpg")
                .estado(estadoFoto)
                .build();
        animal.getFotos().add(foto);
        animalRepository.save(animal);
    }
}
