package com.adoptar.ServicesTests;

import com.adoptar.dto.request.LoginRequest;
import com.adoptar.dto.request.RegisterRequest;
import com.adoptar.dto.response.AuthResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import com.adoptar.exception.DocumentoAlreadyExistsException;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.repository.UserRepository;
import com.adoptar.security.JwtService;
import com.adoptar.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTests {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private User existingUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        existingUser = User.builder()
                .id(1L)
                .nombre("María")
                .apellido("García")
                .dni(12345678L)
                .email("maria@adoptar.com")
                .tel("1122334455")
                .pass("hashedPassword")
                .role(UserRole.USER)
                .build();

        when(userRepository.existsByEmail("maria@adoptar.com")).thenReturn(true);
        when(userRepository.existsByEmail("nuevo@adoptar.com")).thenReturn(false);
        when(userRepository.existsByDni(12345678L)).thenReturn(true);
        when(userRepository.existsByDni(99999999L)).thenReturn(false);
        when(userRepository.findByEmail("maria@adoptar.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(jwtService.generateToken(any())).thenReturn("mockJwtToken");
    }

    @Test
    public void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setDni(99999999L);
        request.setEmail("nuevo@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");

        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegister_Success_WithOrganizacion() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Laura");
        request.setApellido("Gómez");
        request.setDni(99999999L);
        request.setEmail("nuevo@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");
        request.setOrganizacion("Patitas Felices");

        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegister_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setDni(99999999L);
        request.setEmail("maria@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(request);
        });

        assertEquals("Ya existe una cuenta con ese email", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testRegister_DocumentoAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setDni(12345678L);
        request.setEmail("nuevo@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");

        DocumentoAlreadyExistsException exception = assertThrows(DocumentoAlreadyExistsException.class, () -> {
            authService.register(request);
        });

        assertEquals("Ya existe una cuenta con ese documento", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("maria@adoptar.com");
        request.setPass("pass123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.getToken());
        assertEquals("maria@adoptar.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
    }

    @Test
    public void testLogin_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("maria@adoptar.com");
        request.setPass("wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    public void testRegister_NewUserHasRoleUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setDni(99999999L);
        request.setEmail("nuevo@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");

        User savedUser = User.builder()
                .id(2L)
                .nombre("Juan")
                .apellido("Pérez")
                .dni(99999999L)
                .email("nuevo@adoptar.com")
                .tel("1199999999")
                .pass("hashedPassword")
                .role(UserRole.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("mockJwtToken");

        AuthResponse response = authService.register(request);

        assertEquals(UserRole.USER, response.getRole());
    }
}
