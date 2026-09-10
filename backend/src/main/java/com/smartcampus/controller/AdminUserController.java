package com.smartcampus.controller;

import com.smartcampus.dto.AdminResetPasswordRequest;
import com.smartcampus.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String userId,
                                               @Valid @RequestBody AdminResetPasswordRequest request) {
        authService.adminResetPassword(userId, request.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
