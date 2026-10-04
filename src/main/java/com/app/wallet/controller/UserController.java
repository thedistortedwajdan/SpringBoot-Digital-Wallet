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
import com.app.wallet.config.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Users")
@ApiErrorResponses
@RequestMapping("/api/users")
public class UserController {

    private final AuthenticatedUserProvider authenticatedUserProvider;


    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(AuthenticatedUserProvider authenticatedUserProvider, UserService userService) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userService = userService;
    }

    @Operation(summary = "Register a new user (a wallet is created automatically)")
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid
            @RequestBody RegisterUserRequestDto request) {

        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get the authenticated user")
    @GetMapping("/me")
    public UserResponseDto getCurrentUser() {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        return UserResponseDto.from(user);
    }

    @Operation(summary = "Update the authenticated user's profile")
    @PutMapping("/me")
    public UserResponseDto updateCurrentUser(@Valid @RequestBody UpdateUserRequestDto request) {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        return userService.updateUser(user.getId(), request);
    }

    @Operation(summary = "Change the authenticated user's password")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {

        User user = authenticatedUserProvider.getAuthenticatedUser();

        userService.changePassword(user.getId(), request);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Health ping")
    @PostMapping("/ping")
    public void ping() {
        log.info("ping received");
        }
}
