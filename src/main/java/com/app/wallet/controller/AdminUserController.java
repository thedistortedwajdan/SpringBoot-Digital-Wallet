package com.app.wallet.controller;

import com.app.wallet.dto.ChangeRoleRequestDto;
import com.app.wallet.dto.ChangeStatusRequestDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.UpdateUserRequestDto;
import com.app.wallet.dto.UserResponseDto;
import com.app.wallet.service.AuthenticatedUserProvider;
import com.app.wallet.service.UserService;
import jakarta.validation.Valid;
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

    @GetMapping
    public PageResponseDto<UserResponseDto> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(page, size);
    }

    @GetMapping("/{id}")
    public UserResponseDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/role")
    public UserResponseDto changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequestDto request) {
        return userService.changeRole(
                authenticatedUserProvider.getAuthenticatedUser(), id, request.role());
    }

    @PatchMapping("/{id}/status")
    public UserResponseDto changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequestDto request) {
        return userService.changeStatus(
                authenticatedUserProvider.getAuthenticatedUser(), id, request.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(authenticatedUserProvider.getAuthenticatedUser(), id);
        return ResponseEntity.noContent().build();
    }
}
