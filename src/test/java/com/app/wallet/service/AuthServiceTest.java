package com.app.wallet.service;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.exception.AccountDisabledException;
import com.app.wallet.exception.InvalidCredentialsException;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private LoginUserRequestDto request(String email, String password) {
        LoginUserRequestDto dto = new LoginUserRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setEmail("a@test.com");
        user.setFirstName("A");
        user.setLastName("B");
        user.setPassword("hash");
        user.setRole("USER");
        return user;
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        when(userRepository.findUserByEmail("a@test.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(7L)).thenReturn("jwt");

        LoginUserResponseDto response = authService.login(request("a@test.com", "secret"));

        assertThat(response.getAccessToken()).isEqualTo("jwt");
        assertThat(response.getId()).isEqualTo(7L);
    }

    @Test
    void login_withUnknownEmail_failsWithoutCheckingPassword() {
        when(userRepository.findUserByEmail("x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request("x@test.com", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withWrongPassword_fails() {
        when(userRepository.findUserByEmail("a@test.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request("a@test.com", "bad")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_forDeactivatedUser_fails() {
        User inactive = user();
        inactive.setActive(false);
        when(userRepository.findUserByEmail("a@test.com")).thenReturn(Optional.of(inactive));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request("a@test.com", "secret")))
                .isInstanceOf(AccountDisabledException.class);
        verifyNoInteractions(jwtService);
    }
}
