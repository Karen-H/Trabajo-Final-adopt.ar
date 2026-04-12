package com.adoptar.config;

import com.adoptar.entity.Animal;
import com.adoptar.entity.LostFoundPost;
import com.adoptar.entity.User;
import com.adoptar.enums.*;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.LostFoundPostRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialConfiguration implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final LostFoundPostRepository lostFoundPostRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("BD ya inicializada");
            return;
        }

        log.info("Cargando datos iniciales...");
        seedUsers();
        log.info("Datos iniciales cargados correctamente");
    }

    private void seedUsers() {

        // USUARIOS

        User admin = userRepository.save(User.builder()
                .nombre("Admin")
                .apellido("Sistema")
                .documento("00000001")
                .email("admin@adoptar.com")
                .telefono("1100000001")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build());

        User maria = userRepository.save(User.builder()
                .nombre("María")
                .apellido("García")
                .documento("12345678")
                .email("maria@adoptar.com")
                .telefono("1122334455")
                .password(passwordEncoder.encode("pass123"))
                .organizacion("Patitas Felices")
                .role(UserRole.USER)
                .build());

        User carlos = userRepository.save(User.builder()
                .nombre("Carlos")
                .apellido("López")
                .documento("23456789")
                .email("carlos@adoptar.com")
                .telefono("1133445566")
                .password(passwordEncoder.encode("pass123"))
                .role(UserRole.USER)
                .build());

        User ana = userRepository.save(User.builder()
                .nombre("Ana")
                .apellido("Martínez")
                .documento("34567890")
                .email("ana@adoptar.com")
                .telefono("1144556677")
                .password(passwordEncoder.encode("pass123"))
                .role(UserRole.USER)
                .build());

        User laura = userRepository.save(User.builder()
                .nombre("Laura")
                .apellido("Gómez")
                .documento("45678901")
                .email("laura@adoptar.com")
                .telefono("1155667788")
                .password(passwordEncoder.encode("pass123"))
                .organizacion("Rescate Canino Sur")
                .role(UserRole.USER)
                .build());

        log.info("Usuarios creados: {}", userRepository.count());

        // ANIMALES

        animalRepository.save(Animal.builder()
                .nombre("Max")
                .especie(AnimalSpecies.PERRO)
                .sexo("MACHO")
                .edad(2)
                .descripcion("Labrador muy amigable y juguetón. Ideal para familias con niños. Vacunado y desparasitado.")
                .ubicacion("Buenos Aires")
                .friendlyCats(true)
                .friendlyDogs(true)
                .friendlyChildren(true)
                .status(AnimalStatus.EN_ADOPCION)
                .tipo(PublicationType.PERMANENTE)
                .rescatista(maria)
                .build());

        animalRepository.save(Animal.builder()
                .nombre("Luna")
                .especie(AnimalSpecies.GATO)
                .sexo("HEMBRA")
                .edad(1)
                .descripcion("Gata callejera rescatada. Muy cariñosa con adultos. Esterilizada.")
                .ubicacion("Córdoba")
                .friendlyCats(false)
                .friendlyDogs(false)
                .friendlyChildren(true)
                .status(AnimalStatus.EN_ADOPCION)
                .tipo(PublicationType.PERMANENTE)
                .rescatista(maria)
                .build());

        animalRepository.save(Animal.builder()
                .nombre("Rocky")
                .especie(AnimalSpecies.PERRO)
                .sexo("MACHO")
                .edad(3)
                .descripcion("Perro mediano en tránsito. Necesita hogar temporal mientras se busca adoptante definitivo.")
                .ubicacion("Rosario")
                .friendlyCats(false)
                .friendlyDogs(true)
                .friendlyChildren(true)
                .status(AnimalStatus.EN_ADOPCION)
                .tipo(PublicationType.TRANSITO)
                .rescatista(carlos)
                .build());

        animalRepository.save(Animal.builder()
                .nombre("Mishi")
                .especie(AnimalSpecies.GATO)
                .sexo("MACHO")
                .edad(4)
                .descripcion("Gato adulto muy tranquilo. Perfecto para departamento. Castrado.")
                .ubicacion("Buenos Aires")
                .friendlyCats(true)
                .friendlyDogs(false)
                .friendlyChildren(false)
                .status(AnimalStatus.EN_ADOPCION)
                .tipo(PublicationType.PERMANENTE)
                .rescatista(carlos)
                .build());

        animalRepository.save(Animal.builder()
                .nombre("Bella")
                .especie(AnimalSpecies.PERRO)
                .sexo("HEMBRA")
                .edad(1)
                .descripcion("Cachorra mestiza, vacunada y desparasitada. Ya fue adoptada.")
                .ubicacion("Mendoza")
                .friendlyCats(true)
                .friendlyDogs(true)
                .friendlyChildren(true)
                .status(AnimalStatus.ADOPTADO)
                .tipo(PublicationType.PERMANENTE)
                .rescatista(ana)
                .adoptante(carlos)
                .build());

        animalRepository.save(Animal.builder()
                .nombre("Coco")
                .especie(AnimalSpecies.OTRO)
                .sexo("MACHO")
                .edad(2)
                .descripcion("Conejo enano, muy manso. Vive en interior, fácil cuidado.")
                .ubicacion("Buenos Aires")
                .friendlyCats(false)
                .friendlyDogs(false)
                .friendlyChildren(true)
                .status(AnimalStatus.EN_ADOPCION)
                .tipo(PublicationType.PERMANENTE)
                .rescatista(laura)
                .build());

        log.info("Animales creados: {}", animalRepository.count());

        // AVISOS PERDIDOS / ENCONTRADOS

        lostFoundPostRepository.save(LostFoundPost.builder()
                .tipo(LostFoundType.PERDIDO)
                .especie(AnimalSpecies.PERRO)
                .ultimoLugar("Parque Centenario, CABA")
                .ultimaFechaVisto(LocalDate.now().minusDays(3))
                .estadoAnimal(AnimalFoundStatus.SUELTO)
                .nombreContacto("Jorge Sánchez")
                .telefonoContacto("1155667788")
                .emailContacto("jorge@email.com")
                .descripcion("Caniche blanco con collar rojo. Responde al nombre de Toby. Tiene chip.")
                .status(PostStatus.ACTIVO)
                .publisher(ana)
                .build());

        lostFoundPostRepository.save(LostFoundPost.builder()
                .tipo(LostFoundType.ENCONTRADO)
                .especie(AnimalSpecies.GATO)
                .ultimoLugar("Av. Corrientes 3000, CABA")
                .ultimaFechaVisto(LocalDate.now().minusDays(1))
                .estadoAnimal(AnimalFoundStatus.EN_POSESION)
                .nombreContacto("Laura Gómez")
                .telefonoContacto("1166778899")
                .emailContacto("laura@email.com")
                .descripcion("Gato naranja con manchas blancas. Parece doméstico. Lo tengo resguardado en casa.")
                .status(PostStatus.ACTIVO)
                .publisher(carlos)
                .build());

        lostFoundPostRepository.save(LostFoundPost.builder()
                .tipo(LostFoundType.PERDIDO)
                .especie(AnimalSpecies.PERRO)
                .ultimoLugar("Villa Crespo, CABA")
                .ultimaFechaVisto(LocalDate.now().minusDays(7))
                .estadoAnimal(AnimalFoundStatus.SUELTO)
                .nombreContacto("María García")
                .telefonoContacto("1122334455")
                .emailContacto("maria@adoptar.com")
                .descripcion("Golden Retriever hembra, 3 años. Collar azul. Se escapó por obra en la vereda.")
                .status(PostStatus.RESUELTO)
                .publisher(maria)
                .build());

        log.info("Avisos perdidos/encontrados creados: {}", lostFoundPostRepository.count());
    }
}
