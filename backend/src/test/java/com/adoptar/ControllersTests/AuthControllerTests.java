package com.adoptar.ControllersTests;

import com.adoptar.controller.AuthController;
import com.adoptar.dto.request.LoginRequest;
import com.adoptar.dto.request.RegisterRequest;
import com.adoptar.dto.response.AuthResponse;
import com.adoptar.enums.UserRole;
import com.adoptar.exception.DocumentoAlreadyExistsException;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AuthControllerTests {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockAuthResponse = AuthResponse.builder()
                .token("mockJwtToken")
                .id(1L)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@adoptar.com")
                .role(UserRole.USER)
                .build();
    }

    @Test
    public void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setDni(99999999L);
        request.setEmail("juan@adoptar.com");
        request.setTel("1199999999");
        request.setPass("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockAuthResponse, response.getBody());
    }

    @Test
    public void testRegister_EmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existente@adoptar.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Ya existe una cuenta con ese email"));

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testRegister_DocumentoAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setDni(12345678L);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DocumentoAlreadyExistsException("Ya existe una cuenta con ese documento"));

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@adoptar.com");
        request.setPass("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockAuthResponse, response.getBody());
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@adoptar.com");
        request.setPass("wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
