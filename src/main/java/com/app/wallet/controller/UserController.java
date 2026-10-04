package com.app.wallet.controller;

import com.app.wallet.dto.ChangePasswordRequestDto;
import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.dto.UpdateUserRequestDto;
import com.app.wallet.dto.UserResponseDto;
import com.app.wallet.model.User;
import com.app.wallet.service.AuthenticatedUserProvider;
import com.app.wallet.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticatedUserProvider authenticatedUserProvider;


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(AuthenticatedUserProvider authenticatedUserProvider, UserService userService) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid
            @RequestBody RegisterUserRequestDto request) {

        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public UserResponseDto getCurrentUser() {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        return UserResponseDto.from(user);
    }

    @PutMapping("/me")
    public UserResponseDto updateCurrentUser(@Valid @RequestBody UpdateUserRequestDto request) {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        return userService.updateUser(user.getId(), request);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        userService.changePassword(user.getId(), request);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ping")
    public void ping() {
        log.info("ping received");
        }
}
