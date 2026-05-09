package com.adoptar.ServicesTests;

import com.adoptar.dto.request.UpdateProfileRequest;
import com.adoptar.dto.response.UserProfileResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.UserProfile;
import com.adoptar.enums.UserRole;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.exception.TelAlreadyExistsException;
import com.adoptar.repository.UserRepository;
import com.adoptar.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceTests {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(1L)
                .nombre("María")
                .apellido("García")
                .dni(12345678L)
                .email("maria@adoptar.com")
                .tel("1122334455")
                .organizacion(null)
                .role(UserRole.USER)
                .activeProfile(UserProfile.ADOPTANTE)
                .build();
    }

    @Test
    public void testGetProfile_devuelvePerfilCompleto() {
        UserProfileResponse response = userService.getProfile(user);

        assertEquals(user.getId(), response.getId());
        assertEquals(user.getNombre(), response.getNombre());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getTel(), response.getTel());
        assertEquals(user.getRole(), response.getRole());
        assertEquals(UserProfile.ADOPTANTE, response.getActiveProfile());
    }

    @Test
    public void testUpdateProfile_actualizaEmail() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("nuevo@adoptar.com");

        when(userRepository.existsByEmail("nuevo@adoptar.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(user, request);

        assertEquals("nuevo@adoptar.com", response.getEmail());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testUpdateProfile_emailYaExiste_lanzaExcepcion() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("otro@adoptar.com");

        when(userRepository.existsByEmail("otro@adoptar.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.updateProfile(user, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testUpdateProfile_actualizaTelefono() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setTel("1199999999");

        when(userRepository.existsByTel("1199999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(user, request);

        assertEquals("1199999999", response.getTel());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testUpdateProfile_telefonoYaExiste_lanzaExcepcion() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setTel("1100000000");

        when(userRepository.existsByTel("1100000000")).thenReturn(true);

        assertThrows(TelAlreadyExistsException.class, () -> userService.updateProfile(user, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testUpdateProfile_actualizaOrganizacion() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setOrganizacion("Patitas Felices");

        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(user, request);

        assertEquals("Patitas Felices", response.getOrganizacion());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testUpdateProfile_organizacionVacia_guardaNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setOrganizacion("");

        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateProfile(user, request);

        assertNull(user.getOrganizacion());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testSwitchProfile_deAdoptanteARescatista() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.switchProfile(user);

        assertEquals(UserProfile.RESCATISTA, response.getActiveProfile());
    }

    @Test
    public void testSwitchProfile_deRescatistaAAdoptante() {
        user.setActiveProfile(UserProfile.RESCATISTA);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.switchProfile(user);

        assertEquals(UserProfile.ADOPTANTE, response.getActiveProfile());
    }
}
