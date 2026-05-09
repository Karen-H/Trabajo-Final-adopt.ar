package com.adoptar.controller;

import com.adoptar.dto.request.UpdateProfileRequest;
import com.adoptar.dto.response.UserProfileResponse;
import com.adoptar.entity.User;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.exception.TelAlreadyExistsException;
import com.adoptar.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            return ResponseEntity.ok(userService.updateProfile(user, request));
        } catch (EmailAlreadyExistsException | TelAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/profile/switch")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponse> switchProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.switchProfile(user));
    }
}
