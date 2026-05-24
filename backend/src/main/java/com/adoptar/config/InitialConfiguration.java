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
import java.time.LocalDateTime;

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
            seedAnimalesExtra();
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
                .provincia("Ciudad Aut\u00f3noma de Buenos Aires")
                .ciudad("Belgrano")                .organizacion("Patitas Felices CABA")                .build());

        userRepository.save(User.builder()
                .nombre("Karen")
                .apellido("Usuario")
                .dni(1000003L)
                .email("karen@adoptar.com")
                .tel("1100000003")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.USER)
                .provincia("Ciudad Aut\u00f3noma de Buenos Aires")
                .ciudad("Retiro")                .organizacion("Amigos Peludos")                .build());

        // 48 usuarios con ubicaciones y fechas diversas de toda Argentina
        crearUsuario("Valentina", "Gonz\u00e1lez",  1000006L, "valentina.gonzalez@adoptar.com", "1100000006", "Buenos Aires",                    "La Plata",                            LocalDateTime.of(2024,  1, 15, 10,  0));
        crearUsuario("Mart\u00edn",    "Rodr\u00edguez", 1000007L, "martin.rodriguez@adoptar.com",    "1100000007", "Ciudad Aut\u00f3noma de Buenos Aires", "Palermo",                             LocalDateTime.of(2024,  2, 20, 14, 30));
        crearUsuario("Camila",    "Garc\u00eda",    1000008L, "camila.garcia@adoptar.com",        "1100000008", "Santa Fe",                        "Rosario",                             LocalDateTime.of(2024,  3,  8,  9, 15));
        crearUsuario("Santiago",  "Fern\u00e1ndez", 1000009L, "santiago.fernandez@adoptar.com",   "1100000009", "C\u00f3rdoba",                         "C\u00f3rdoba",                               LocalDateTime.of(2024,  4, 12, 11,  0));
        crearUsuario("Florencia", "L\u00f3pez",     1000010L, "florencia.lopez@adoptar.com",      "1100000010", "Mendoza",                         "Mendoza",                             LocalDateTime.of(2024,  5,  5, 16, 45));
        crearUsuario("Nicol\u00e1s",   "Mart\u00ednez",  1000011L, "nicolas.martinez@adoptar.com",    "1100000011", "Salta",                           "Salta",                               LocalDateTime.of(2024,  6, 18,  8, 20));
        crearUsuario("Luciana",   "S\u00e1nchez",   1000012L, "luciana.sanchez@adoptar.com",      "1100000012", "Misiones",                        "Posadas",                             LocalDateTime.of(2024,  7, 22, 13, 10));
        crearUsuario("Diego",     "P\u00e9rez",     1000013L, "diego.perez@adoptar.com",          "1100000013", "Entre R\u00edos",                      "Paran\u00e1",                              LocalDateTime.of(2024,  8, 14, 17,  0));
        crearUsuario("Natalia",   "G\u00f3mez",     1000014L, "natalia.gomez@adoptar.com",        "1100000014", "Tucum\u00e1n",                         "San Miguel de Tucum\u00e1n",               LocalDateTime.of(2024,  9,  3, 10, 30));
        crearUsuario("Facundo",   "D\u00edaz",      1000015L, "facundo.diaz@adoptar.com",         "1100000015", "Neuqu\u00e9n",                         "Neuqu\u00e9n",                               LocalDateTime.of(2024, 10, 27, 12,  0));
        crearUsuario("Daniela",   "Torres",    1000016L, "daniela.torres@adoptar.com",       "1100000016", "R\u00edo Negro",                       "Bariloche",                           LocalDateTime.of(2024, 11, 11, 15, 45));
        crearUsuario("Rodrigo",   "Flores",    1000017L, "rodrigo.flores@adoptar.com",       "1100000017", "Chubut",                          "Comodoro Rivadavia",                  LocalDateTime.of(2024, 12,  5,  9,  0));
        crearUsuario("Ver\u00f3nica",  "Moreno",    1000018L, "veronica.moreno@adoptar.com",      "1100000018", "Corrientes",                      "Corrientes",                          LocalDateTime.of(2025,  1, 19, 11, 20));
        crearUsuario("Sebasti\u00e1n", "Jim\u00e9nez",   1000019L, "sebastian.jimenez@adoptar.com",    "1100000019", "Chaco",                           "Resistencia",                         LocalDateTime.of(2025,  2,  7, 14,  0));
        crearUsuario("Claudia",   "Vargas",    1000020L, "claudia.vargas@adoptar.com",       "1100000020", "Tierra del Fuego",                "Ushuaia",                             LocalDateTime.of(2025,  3, 14, 16, 30));
        crearUsuario("Mat\u00edas",    "Castro",    1000021L, "matias.castro@adoptar.com",        "1100000021", "San Juan",                        "San Juan",                            LocalDateTime.of(2025,  4, 21,  8, 45));
        crearUsuario("Alejandra", "R\u00edos",      1000022L, "alejandra.rios@adoptar.com",       "1100000022", "La Rioja",                        "La Rioja",                            LocalDateTime.of(2025,  5, 30, 10,  0));
        crearUsuario("Federico",  "Herrera",   1000023L, "federico.herrera@adoptar.com",     "1100000023", "Jujuy",                           "San Salvador de Jujuy",               LocalDateTime.of(2025,  6,  9, 13, 15));
        crearUsuario("Romina",    "Medina",    1000024L, "romina.medina@adoptar.com",        "1100000024", "Formosa",                         "Formosa",                             LocalDateTime.of(2025,  7, 17, 17, 30));
        crearUsuario("Agust\u00edn",   "Su\u00e1rez",    1000025L, "agustin.suarez@adoptar.com",       "1100000025", "C\u00f3rdoba",                         "Villa Carlos Paz",                    LocalDateTime.of(2025,  8, 25,  9, 20));
        crearUsuario("Silvana",   "Molina",    1000026L, "silvana.molina@adoptar.com",       "1100000026", "Buenos Aires",                    "Mar del Plata",                       LocalDateTime.of(2025,  9, 13, 12,  0));
        crearUsuario("Emiliano",  "Romero",    1000027L, "emiliano.romero@adoptar.com",      "1100000027", "Ciudad Aut\u00f3noma de Buenos Aires", "Recoleta",                            LocalDateTime.of(2025, 10,  2, 14, 45));
        crearUsuario("Paola",     "N\u00fa\u00f1ez",     1000028L, "paola.nunez@adoptar.com",          "1100000028", "Mendoza",                         "Godoy Cruz",                          LocalDateTime.of(2025, 10, 28, 11,  0));
        crearUsuario("Pablo",     "Blanco",    1000029L, "pablo.blanco@adoptar.com",         "1100000029", "Buenos Aires",                    "Quilmes",                             LocalDateTime.of(2025, 11,  6, 16,  0));
        crearUsuario("Mariana",   "Vega",      1000030L, "mariana.vega@adoptar.com",         "1100000030", "Entre R\u00edos",                      "Gualeguaych\u00fa",                        LocalDateTime.of(2025, 11, 20,  8, 30));
        crearUsuario("Gonzalo",   "Cabrera",   1000031L, "gonzalo.cabrera@adoptar.com",      "1100000031", "C\u00f3rdoba",                         "R\u00edo Cuarto",                           LocalDateTime.of(2025, 12,  3, 10, 15));
        crearUsuario("Cecilia",   "Reyes",     1000032L, "cecilia.reyes@adoptar.com",        "1100000032", "Ciudad Aut\u00f3noma de Buenos Aires", "Villa Urquiza",                       LocalDateTime.of(2025, 12, 18, 13,  0));
        crearUsuario("Alejandro", "Mendoza",   1000033L, "alejandro.mendoza@adoptar.com",    "1100000033", "Buenos Aires",                    "Bah\u00eda Blanca",                        LocalDateTime.of(2026,  1,  7, 17, 20));
        crearUsuario("Vanesa",    "Silva",     1000034L, "vanesa.silva@adoptar.com",         "1100000034", "Misiones",                        "Ober\u00e1",                                LocalDateTime.of(2026,  1, 22,  9, 45));
        crearUsuario("Hern\u00e1n",    "Mu\u00f1oz",     1000035L, "hernan.munoz@adoptar.com",         "1100000035", "La Pampa",                        "Santa Rosa",                          LocalDateTime.of(2026,  2,  4, 11, 30));
        crearUsuario("Jimena",    "Gonz\u00e1lez",  1000036L, "jimena.gonzalez@adoptar.com",      "1100000036", "Neuqu\u00e9n",                         "San Mart\u00edn de los Andes",              LocalDateTime.of(2026,  2, 15, 14,  0));
        crearUsuario("Javier",    "Rodr\u00edguez", 1000037L, "javier.rodriguez@adoptar.com",     "1100000037", "Chubut",                          "Puerto Madryn",                       LocalDateTime.of(2026,  3,  1,  8,  0));
        crearUsuario("Lorena",    "Garc\u00eda",    1000038L, "lorena.garcia@adoptar.com",        "1100000038", "Salta",                           "Or\u00e1n",                                LocalDateTime.of(2026,  3,  8, 10, 45));
        crearUsuario("Carlos",    "Fern\u00e1ndez", 1000039L, "carlos.fernandez@adoptar.com",     "1100000039", "Ciudad Aut\u00f3noma de Buenos Aires", "Caballito",                           LocalDateTime.of(2026,  3, 14, 15, 30));
        crearUsuario("Gabriela",  "L\u00f3pez",     1000040L, "gabriela.lopez@adoptar.com",       "1100000040", "Santa Fe",                        "Santa Fe",                            LocalDateTime.of(2026,  3, 21, 12, 15));
        crearUsuario("Eduardo",   "Mart\u00ednez",  1000041L, "eduardo.martinez@adoptar.com",     "1100000041", "Entre R\u00edos",                      "Concordia",                           LocalDateTime.of(2026,  3, 28,  9, 30));
        crearUsuario("Valeria",   "S\u00e1nchez",   1000042L, "valeria.sanchez@adoptar.com",      "1100000042", "Tucum\u00e1n",                         "Taf\u00ed Viejo",                           LocalDateTime.of(2026,  4,  4, 16,  0));
        crearUsuario("Tom\u00e1s",     "P\u00e9rez",     1000043L, "tomas.perez@adoptar.com",          "1100000043", "Corrientes",                      "Goya",                                LocalDateTime.of(2026,  4, 10, 11, 45));
        crearUsuario("Patricia",  "G\u00f3mez",     1000044L, "patricia.gomez@adoptar.com",       "1100000044", "San Luis",                        "San Luis",                            LocalDateTime.of(2026,  4, 16, 14, 20));
        crearUsuario("Felipe",    "D\u00edaz",      1000045L, "felipe.diaz@adoptar.com",          "1100000045", "Jujuy",                           "Palpal\u00e1",                              LocalDateTime.of(2026,  4, 22,  8, 10));
        crearUsuario("Ramiro",    "Torres",    1000046L, "ramiro.torres@adoptar.com",        "1100000046", "Buenos Aires",                    "Tandil",                              LocalDateTime.of(2026,  4, 28, 13, 30));
        crearUsuario("Andrea",    "Flores",    1000047L, "andrea.flores@adoptar.com",        "1100000047", "Ciudad Aut\u00f3noma de Buenos Aires", "Flores",                              LocalDateTime.of(2026,  5,  3, 10,  0));
        crearUsuario("Leonardo",  "Moreno",    1000048L, "leonardo.moreno@adoptar.com",      "1100000048", "R\u00edo Negro",                       "Viedma",                              LocalDateTime.of(2026,  5,  8, 15, 15));
        crearUsuario("Cristian",  "Jim\u00e9nez",   1000049L, "cristian.jimenez@adoptar.com",     "1100000049", "Chaco",                           "Presidencia Roque S\u00e1enz Pe\u00f1a",        LocalDateTime.of(2026,  5, 10,  9, 45));
        crearUsuario("Esteban",   "Vargas",    1000050L, "esteban.vargas@adoptar.com",       "1100000050", "Mendoza",                         "San Rafael",                          LocalDateTime.of(2026,  5, 12, 11, 20));
        crearUsuario("Gustavo",   "Castro",    1000051L, "gustavo.castro@adoptar.com",       "1100000051", "Catamarca",                       "San Fernando del Valle de Catamarca", LocalDateTime.of(2026,  5, 14, 16, 45));
        crearUsuario("Mario",     "R\u00edos",      1000052L, "mario.rios@adoptar.com",           "1100000052", "San Luis",                        "Villa Mercedes",                      LocalDateTime.of(2026,  5, 16,  8, 30));
        crearUsuario("Andr\u00e9s",    "Herrera",   1000053L, "andres.herrera@adoptar.com",       "1100000053", "C\u00f3rdoba",                         "Alta Gracia",                         LocalDateTime.of(2026,  5, 18, 12,  0));

        log.info("Usuarios seed: 1 admin + 2 base (pass: admin123) + 48 extra (pass: user123)");
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
                .adoptadoEn(LocalDateTime.of(2026, 3, 5, 14, 0))
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
                .adoptadoEn(LocalDateTime.of(2026, 4, 12, 10, 30))
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
                .adoptadoEn(LocalDateTime.of(2026, 5, 1, 9, 0))
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.PERDIDO)
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
                .estadoInicial(EstadoAnimal.ENCONTRADO)
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
                .estadoInicial(EstadoAnimal.ENCONTRADO)
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
                .estadoInicial(EstadoAnimal.ENCONTRADO)
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

    private void seedAnimalesExtra() {
        // usuarios 1-10: 1 animal perdido cada uno
        String[] emailsPerdidos = {
            "valentina.gonzalez@adoptar.com", "martin.rodriguez@adoptar.com",
            "camila.garcia@adoptar.com",      "santiago.fernandez@adoptar.com",
            "florencia.lopez@adoptar.com",    "nicolas.martinez@adoptar.com",
            "luciana.sanchez@adoptar.com",    "diego.perez@adoptar.com",
            "natalia.gomez@adoptar.com",      "facundo.diaz@adoptar.com"
        };
        // usuarios 11-20: 1 animal encontrado cada uno
        String[] emailsEncontrados = {
            "daniela.torres@adoptar.com",  "rodrigo.flores@adoptar.com",
            "veronica.moreno@adoptar.com", "sebastian.jimenez@adoptar.com",
            "claudia.vargas@adoptar.com",  "matias.castro@adoptar.com",
            "alejandra.rios@adoptar.com",  "federico.herrera@adoptar.com",
            "romina.medina@adoptar.com",   "agustin.suarez@adoptar.com"
        };
        // usuarios 21-48: 10 animales en adopción cada uno
        String[] emailsAdopcion = {
            "silvana.molina@adoptar.com",    "emiliano.romero@adoptar.com",
            "paola.nunez@adoptar.com",       "pablo.blanco@adoptar.com",
            "mariana.vega@adoptar.com",      "gonzalo.cabrera@adoptar.com",
            "cecilia.reyes@adoptar.com",     "alejandro.mendoza@adoptar.com",
            "vanesa.silva@adoptar.com",      "hernan.munoz@adoptar.com",
            "jimena.gonzalez@adoptar.com",   "javier.rodriguez@adoptar.com",
            "lorena.garcia@adoptar.com",     "carlos.fernandez@adoptar.com",
            "gabriela.lopez@adoptar.com",    "eduardo.martinez@adoptar.com",
            "valeria.sanchez@adoptar.com",   "tomas.perez@adoptar.com",
            "patricia.gomez@adoptar.com",    "felipe.diaz@adoptar.com",
            "ramiro.torres@adoptar.com",     "andrea.flores@adoptar.com",
            "leonardo.moreno@adoptar.com",   "cristian.jimenez@adoptar.com",
            "esteban.vargas@adoptar.com",    "gustavo.castro@adoptar.com",
            "mario.rios@adoptar.com",        "andres.herrera@adoptar.com"
        };

        TipoAnimal[]  tiposPE  = {TipoAnimal.PERRO, TipoAnimal.GATO,  TipoAnimal.PERRO, TipoAnimal.GATO,
                                   TipoAnimal.PERRO, TipoAnimal.GATO,  TipoAnimal.OTRO,  TipoAnimal.PERRO,
                                   TipoAnimal.GATO,  TipoAnimal.PERRO};
        SexoAnimal[]  sexosPE  = {SexoAnimal.MACHO,  SexoAnimal.HEMBRA, SexoAnimal.HEMBRA, SexoAnimal.MACHO,
                                   SexoAnimal.MACHO,  SexoAnimal.HEMBRA, SexoAnimal.MACHO,  SexoAnimal.HEMBRA,
                                   SexoAnimal.MACHO,  SexoAnimal.HEMBRA};
        RangoEdad[]   edadesPE = {RangoEdad.ADULTO, RangoEdad.JOVEN,  RangoEdad.ADULTO, RangoEdad.ADULTO,
                                   RangoEdad.JOVEN,  RangoEdad.ADULTO, RangoEdad.ADULTO, RangoEdad.CACHORRO,
                                   RangoEdad.ADULTO, RangoEdad.JOVEN};
        double[] lats = {-34.92, -32.95, -31.42, -32.89, -24.78, -31.74, -26.82, -38.95, -41.13, -27.47};
        double[] lons = {-57.95, -60.66, -64.19, -68.83, -65.41, -60.52, -65.22, -68.06, -71.31, -58.83};
        String[] provsPE   = {"Buenos Aires", "Santa Fe", "Córdoba",  "Mendoza", "Salta",
                               "Entre Ríos",   "Tucumán",   "Neuquén",  "Río Negro", "Corrientes"};
        String[] ciudadesPE = {"La Plata", "Rosario", "Córdoba", "Mendoza", "Salta",
                                "Paraná",   "San Miguel de Tucumán", "Neuquén", "Bariloche", "Corrientes"};
        String[] dirs = {"Av. 7 esq. 50", "Corrientes 1500", "Gral. Paz 200",   "San Martín 800",
                          "Belgrano 300",  "Alameda 600",     "25 de Mayo 1000", "Av. Argentina 400",
                          "Mitre 500",     "San Martín 200"};
        String[] descPerd = {
            "Se perdió en el parque. Tiene collar azul.",
            "Gata atigrada desapareció del jardín.",
            "Perro labrador muy dócil. Se escapó por la puerta.",
            "Gato negro con collar rojo. Desapareció de noche.",
            "Pastor alemán joven, responde al nombre Trueno.",
            "Gata siamesa, muy tímida. Se escapó por la ventana.",
            "Se perdió en zona rural, muy dócil y sin agresor.",
            "Cachorro mestizo, pelaje marrón claro. Tiene microchip.",
            "Gato anaranjado con mancha blanca en el pecho.",
            "Border collie adulto, collar GPS sin batería."
        };
        String[] descEnc = {
            "Perro encontrado deambulando sin collar.",
            "Gata encontrada en la calle, muy asustada.",
            "Perro mestizo encontrado cerca de la ruta.",
            "Gatito encontrado en un árbol, muy asustado.",
            "Perro encontrado herido leve, ya atendido por veterinario.",
            "Gata preñada encontrada en el patio de una casa.",
            "Animal encontrado en zona rural, manso y dócil.",
            "Cachorro encontrado abandonado dentro de una caja.",
            "Gato adulto encontrado dentro de un restaurante.",
            "Perro anciano encontrado sin chip ni collar."
        };

        for (int i = 0; i < emailsPerdidos.length; i++) {
            User u = userRepository.findByEmail(emailsPerdidos[i]).orElseThrow();
            boolean resuelto = i < 5;
            Animal.AnimalBuilder bld = Animal.builder()
                    .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                    .tipo(tiposPE[i]).sexo(sexosPE[i]).edad(edadesPE[i])
                    .estado(resuelto ? EstadoAnimal.RESUELTO : EstadoAnimal.PERDIDO)
                    .estadoInicial(EstadoAnimal.PERDIDO)
                    .descripcion(descPerd[i])
                    .direccion(dirs[i]).latitud(lats[i]).longitud(lons[i])
                    .provincia(provsPE[i]).ciudad(ciudadesPE[i])
                    .fechaAvistamiento(LocalDate.of(2026, 5, i + 1))
                    .enPosesionDelPublicador(false)
                    .aprobado(true).publicador(u);
            if (resuelto) {
                bld.resueltoEn(LocalDateTime.of(2026, 1 + (i % 5), 15, 12, 0));
            }
            guardar(bld.build(), EstadoFoto.APROBADA);
        }

        for (int i = 0; i < emailsEncontrados.length; i++) {
            User u = userRepository.findByEmail(emailsEncontrados[i]).orElseThrow();
            boolean resuelto = i < 5;
            Animal.AnimalBuilder bld = Animal.builder()
                    .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                    .tipo(tiposPE[(i + 4) % 10]).sexo(sexosPE[(i + 3) % 10]).edad(edadesPE[(i + 2) % 10])
                    .estado(resuelto ? EstadoAnimal.RESUELTO : EstadoAnimal.ENCONTRADO)
                    .estadoInicial(EstadoAnimal.ENCONTRADO)
                    .descripcion(descEnc[i])
                    .direccion(dirs[(i + 5) % 10]).latitud(lats[(i + 5) % 10]).longitud(lons[(i + 5) % 10])
                    .provincia(provsPE[(i + 5) % 10]).ciudad(ciudadesPE[(i + 5) % 10])
                    .fechaAvistamiento(LocalDate.of(2026, 5, i + 1))
                    .enPosesionDelPublicador(i % 2 == 0)
                    .aprobado(true).publicador(u);
            if (resuelto) {
                bld.resueltoEn(LocalDateTime.of(2026, 6 + (i % 5), 20, 14, 0));
            }
            guardar(bld.build(), EstadoFoto.APROBADA);
        }

        // combinaciones para adopción (10 plantillas rotadas por usuario)
        TipoAnimal[]  tipos      = {TipoAnimal.PERRO, TipoAnimal.GATO,  TipoAnimal.OTRO,  TipoAnimal.PERRO,
                                     TipoAnimal.GATO,  TipoAnimal.PERRO, TipoAnimal.GATO,  TipoAnimal.OTRO,
                                     TipoAnimal.PERRO, TipoAnimal.GATO};
        RangoEdad[]   edades     = {RangoEdad.CACHORRO, RangoEdad.JOVEN,    RangoEdad.ADULTO, RangoEdad.ADULTO,
                                     RangoEdad.SENIOR,   RangoEdad.JOVEN,    RangoEdad.CACHORRO, RangoEdad.ADULTO,
                                     RangoEdad.SENIOR,   RangoEdad.JOVEN};
        TipoAdopcion[] tiposAdop = {TipoAdopcion.PERMANENTE, TipoAdopcion.TRANSITO,   TipoAdopcion.PERMANENTE,
                                     TipoAdopcion.PERMANENTE, TipoAdopcion.TRANSITO,   TipoAdopcion.TRANSITO,
                                     TipoAdopcion.PERMANENTE, TipoAdopcion.PERMANENTE, TipoAdopcion.TRANSITO,
                                     TipoAdopcion.PERMANENTE};
        SexoAnimal[]  sexos      = {SexoAnimal.MACHO,  SexoAnimal.HEMBRA, SexoAnimal.HEMBRA, SexoAnimal.MACHO,
                                     SexoAnimal.HEMBRA, SexoAnimal.MACHO,  SexoAnimal.HEMBRA, SexoAnimal.MACHO,
                                     SexoAnimal.HEMBRA, SexoAnimal.MACHO};
        boolean[] amigGatos  = {false, true,  false, true,  false, false, true,  false, true,  false};
        boolean[] amigPerros = {true,  false, true,  false, true,  true,  false, true,  false, true};
        boolean[] amigNinos  = {true,  true,  false, true,  false, true,  true,  false, true,  true};
        String[] nombresA = {
            "Max", "Luna", "Rocky", "Mia",   "Thor",  "Bella", "Leo",  "Nina",  "Simba", "Nala",
            "Bruno", "Cleo", "Rex", "Mochi", "Apolo", "Lola",  "Zeus", "Kitty", "Toby",  "Perla",
            "Nico", "Gala", "Tito", "Sasha", "Pato",  "Coco",  "Fido", "Mora",  "Gordo", "Cuca"
        };
        String[] descsA = {
            "Animal muy cariñoso y tranquilo, ideal para familia con niños.",
            "Busca un hogar amoroso. Está vacunado y desparasitado.",
            "Muy jugletón y activo. Necesita espacio para correr.",
            "Tranquilo y dócil. Perfecto para vivir en departamento.",
            "Un poco tímido al principio pero muy afectuoso con su familia.",
            "Sociable con otros animales. Le encanta jugar.",
            "Rescatado de la calle. Ya recuperado y listo para adoptar.",
            "Muy inteligente y obediente. Sabe algunos comandos básicos.",
            "Mayor pero muy dulce. Busca un hogar tranquilo.",
            "Energético y divertido. Ideal para personas activas."
        };

        // primeros 25 usuarios: 4 animales adoptados cada uno (25×4 = 100); resto: EN_ADOPCION
        for (int u = 0; u < emailsAdopcion.length; u++) {
            User usuario = userRepository.findByEmail(emailsAdopcion[u]).orElseThrow();
            for (int a = 0; a < 10; a++) {
                int idx = (u + a) % 10;
                boolean adoptado = u < 25 && a < 4;
                int posicion = u * 4 + a; // para variar la fecha
                Animal.AnimalBuilder builder = Animal.builder()
                        .categoria(CategoriaAnimal.ADOPCION)
                        .nombre(nombresA[(u * 10 + a) % nombresA.length])
                        .sexo(sexos[idx]).edad(edades[idx]).tipo(tipos[idx])
                        .tipoAdopcion(tiposAdop[idx])
                        .estado(adoptado ? EstadoAnimal.ADOPTADO : EstadoAnimal.EN_ADOPCION)
                        .amigableConGatos(amigGatos[idx])
                        .amigableConPerros(amigPerros[idx])
                        .amigableConNinos(amigNinos[idx])
                        .descripcion(descsA[a])
                        .aprobado(true).publicador(usuario);
                if (adoptado) {
                    // Distribución irregular entre May 2025 – May 2026 usando función no lineal
                    int h = (posicion * 43 + posicion * posicion * 3) % 390;
                    int diaEnAnio = 121 + h;
                    int anio = 2025;
                    if (diaEnAnio > 365) { anio = 2026; diaEnAnio -= 365; }
                    int[] inicio = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
                    int mes = 1;
                    for (int m = 11; m >= 0; m--) {
                        if (diaEnAnio > inicio[m]) { mes = m + 1; break; }
                    }
                    int dia = Math.max(1, Math.min(diaEnAnio - inicio[mes - 1], 28));
                    builder.adoptadoEn(LocalDateTime.of(anio, mes, dia, 10 + (posicion % 8), 0));
                }
                guardar(builder.build(), EstadoFoto.APROBADA);
            }
        }

        log.info("Animales extra seed: 10 perdidos + 10 encontrados + 280 en adopción (100 adoptados)");
    }

    private void crearUsuario(String nombre, String apellido, long dni, String email, String tel,
                               String provincia, String ciudad, LocalDateTime createdAt) {
        userRepository.save(User.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .email(email)
                .tel(tel)
                .pass(passwordEncoder.encode("user123"))
                .role(UserRole.USER)
                .provincia(provincia)
                .ciudad(ciudad)
                .organizacion("Org. " + nombre + " " + apellido)
                .createdAt(createdAt)
                .build());
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
