package com.app.wallet.controller;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid
            @RequestBody RegisterUserRequestDto request) {

        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/ping")
    public void ping() {
        log.info("ping received");
        }
}