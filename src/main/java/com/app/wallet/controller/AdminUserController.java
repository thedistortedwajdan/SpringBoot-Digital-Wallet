package com.app.wallet.controller;

import com.app.wallet.dto.ChangeRoleRequestDto;
import com.app.wallet.dto.ChangeStatusRequestDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.UpdateUserRequestDto;
import com.app.wallet.dto.UserResponseDto;
import com.app.wallet.service.AuthenticatedUserProvider;
import com.app.wallet.service.UserService;
import jakarta.validation.Valid;
import com.app.wallet.config.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin - Users")
@ApiErrorResponses
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AdminUserController(UserService userService,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.userService = userService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Operation(summary = "List all users (ADMIN)")
    @GetMapping
    public PageResponseDto<UserResponseDto> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(page, size);
    }

    @Operation(summary = "Get a user by id (ADMIN)")
    @GetMapping("/{id}")
    public UserResponseDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @Operation(summary = "Update a user (ADMIN)")
    @PutMapping("/{id}")
    public UserResponseDto updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        return userService.updateUser(id, request);
    }

    @Operation(summary = "Change a user's role (ADMIN)")
    @PatchMapping("/{id}/role")
    public UserResponseDto changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequestDto request) {
        return userService.changeRole(
                authenticatedUserProvider.getAuthenticatedUser(), id, request.role());
    }

    @Operation(summary = "Activate or deactivate a user (ADMIN)")
    @PatchMapping("/{id}/status")
    public UserResponseDto changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequestDto request) {
        return userService.changeStatus(
                authenticatedUserProvider.getAuthenticatedUser(), id, request.active());
    }

    @Operation(summary = "Delete a user without wallet activity (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(authenticatedUserProvider.getAuthenticatedUser(), id);
        return ResponseEntity.noContent().build();
    }
}
