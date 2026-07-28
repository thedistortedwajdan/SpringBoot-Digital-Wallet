package com.app.wallet.controller;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    LoginUserResponseDto login(@Valid @RequestBody LoginUserRequestDto request)
    {

        return authService.login(request);
    }
}
