package com.adoptar.service;

import com.adoptar.dto.request.UpdateProfileRequest;
import com.adoptar.dto.response.UserProfileResponse;
import com.adoptar.entity.User;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .dni(user.getDni())
                .email(user.getEmail())
                .tel(user.getTel())
                .organizacion(user.getOrganizacion())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException("Ya existe una cuenta con ese email");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getTel() != null && !request.getTel().isBlank()) {
            user.setTel(request.getTel());
        }

        if (request.getOrganizacion() != null) {
            user.setOrganizacion(request.getOrganizacion().isBlank() ? null : request.getOrganizacion());
        }

        userRepository.save(user);
        return getProfile(user);
    }
}
