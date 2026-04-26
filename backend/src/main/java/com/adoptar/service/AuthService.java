package com.adoptar.service;

import com.adoptar.dto.request.LoginRequest;
import com.adoptar.dto.request.RegisterRequest;
import com.adoptar.dto.response.AuthResponse;
import com.adoptar.entity.User;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.exception.DocumentoAlreadyExistsException;
import com.adoptar.repository.UserRepository;
import com.adoptar.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Ya existe una cuenta con ese email");
        }
        if (userRepository.existsByDni(request.getDni())) {
            throw new DocumentoAlreadyExistsException("Ya existe una cuenta con ese documento");
        }

        User user = User.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .dni(request.getDni())
                .email(request.getEmail())
                .tel(request.getTel())
                .pass(passwordEncoder.encode(request.getPass()))
                .organizacion(request.getOrganizacion())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPass())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
