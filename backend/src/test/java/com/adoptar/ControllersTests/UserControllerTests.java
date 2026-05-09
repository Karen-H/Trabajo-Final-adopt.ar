package com.adoptar.ControllersTests;

import com.adoptar.controller.UserController;
import com.adoptar.dto.request.UpdateProfileRequest;
import com.adoptar.dto.response.UserProfileResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.UserProfile;
import com.adoptar.enums.UserRole;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.exception.TelAlreadyExistsException;
import com.adoptar.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserControllerTests {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private User user;
    private UserProfileResponse mockProfile;

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
                .role(UserRole.USER)
                .activeProfile(UserProfile.ADOPTANTE)
                .build();

        mockProfile = UserProfileResponse.builder()
                .id(1L)
                .nombre("María")
                .apellido("García")
                .dni(12345678L)
                .email("maria@adoptar.com")
                .tel("1122334455")
                .role(UserRole.USER)
                .activeProfile(UserProfile.ADOPTANTE)
                .build();
    }

    @Test
    public void testGetProfile_devuelveOk() {
        when(userService.getProfile(user)).thenReturn(mockProfile);

        ResponseEntity<UserProfileResponse> response = userController.getProfile(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockProfile, response.getBody());
    }

    @Test
    public void testUpdateProfile_devuelveOk() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("nuevo@adoptar.com");

        when(userService.updateProfile(any(User.class), any(UpdateProfileRequest.class))).thenReturn(mockProfile);

        ResponseEntity<UserProfileResponse> response = userController.updateProfile(user, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockProfile, response.getBody());
    }

    @Test
    public void testUpdateProfile_emailConflicto_devuelve409() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("otro@adoptar.com");

        when(userService.updateProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Ya existe una cuenta con ese email"));

        ResponseEntity<UserProfileResponse> response = userController.updateProfile(user, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testUpdateProfile_telefonoConflicto_devuelve409() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setTel("1100000000");

        when(userService.updateProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new TelAlreadyExistsException("Ya existe una cuenta con ese teléfono"));

        ResponseEntity<UserProfileResponse> response = userController.updateProfile(user, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testSwitchProfile_devuelveOk() {
        UserProfileResponse perfilRescatista = UserProfileResponse.builder()
                .id(1L)
                .nombre("María")
                .apellido("García")
                .dni(12345678L)
                .email("maria@adoptar.com")
                .tel("1122334455")
                .role(UserRole.USER)
                .activeProfile(UserProfile.RESCATISTA)
                .build();

        when(userService.switchProfile(user)).thenReturn(perfilRescatista);

        ResponseEntity<UserProfileResponse> response = userController.switchProfile(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(UserProfile.RESCATISTA, response.getBody().getActiveProfile());
    }
}
