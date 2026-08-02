package com.app.wallet.controller;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    LoginUserResponseDto login(@Valid @RequestBody LoginUserRequestDto request)
    {
        log.info("calling login");
        return authService.login(request);
    }
}
