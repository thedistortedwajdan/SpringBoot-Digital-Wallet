package com.app.wallet.service;

import com.app.wallet.dto.ChangePasswordRequestDto;
import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.exception.BadRequestException;
import com.app.wallet.exception.ConflictException;
import com.app.wallet.exception.EmailAlreadyExistsException;
import com.app.wallet.exception.UserDoesNotExistException;
import com.app.wallet.model.Role;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterUserRequestDto registerRequest() {
        RegisterUserRequestDto dto = new RegisterUserRequestDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("a@test.com");
        dto.setPassword("password123");
        return dto;
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("u" + id + "@test.com");
        user.setPassword("hash");
        user.setRole("USER");
        return user;
    }

    @Test
    void register_hashesPasswordAssignsUserRoleAndCreatesWallet() {
        when(userRepository.existsByEmail("a@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.createUser(any(User.class))).thenReturn(5L);

        userService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).createUser(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
        verify(walletRepository).createWallet(5L);
    }

    @Test
    void register_withExistingEmail_throwsConflict() {
        when(userRepository.existsByEmail("a@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).createUser(any());
    }

    @Test
    void register_whenRaceHitsUniqueConstraint_throwsConflictAndSkipsWallet() {
        when(userRepository.existsByEmail("a@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.createUser(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> userService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(walletRepository, never()).createWallet(any());
    }

    @Test
    void changePassword_withWrongCurrentPassword_isRejected() {
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequestDto("wrong", "newpassword1")))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).updatePassword(any(), any());
    }

    @Test
    void changePassword_storesHashOfNewPassword() {
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("current123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("newpassword1", "hash")).thenReturn(false);
        when(passwordEncoder.encode("newpassword1")).thenReturn("newhash");

        userService.changePassword(1L, new ChangePasswordRequestDto("current123", "newpassword1"));

        verify(userRepository).updatePassword(1L, "newhash");
    }

    @Test
    void changeRole_onSelf_isRejected() {
        assertThatThrownBy(() -> userService.changeRole(user(1), 1L, Role.USER))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).updateRole(any(), any());
    }

    @Test
    void changeRole_forUnknownUser_throwsNotFound() {
        when(userRepository.findUserById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole(user(1), 2L, Role.ADMIN))
                .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    void deleteUser_withWalletActivity_isRejected() {
        when(userRepository.findUserById(2L)).thenReturn(Optional.of(user(2)));
        when(walletRepository.hasActivity(2L)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(user(1), 2L))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_withoutActivity_deletes() {
        when(userRepository.findUserById(2L)).thenReturn(Optional.of(user(2)));
        when(walletRepository.hasActivity(2L)).thenReturn(false);

        userService.deleteUser(user(1), 2L);

        verify(userRepository).deleteById(2L);
    }
}
