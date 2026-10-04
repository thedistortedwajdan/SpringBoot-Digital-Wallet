package com.app.wallet.service;

import com.app.wallet.dto.ChangePasswordRequestDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.dto.UpdateUserRequestDto;
import com.app.wallet.dto.UserResponseDto;
import com.app.wallet.exception.BadRequestException;
import com.app.wallet.exception.ConflictException;
import com.app.wallet.exception.EmailAlreadyExistsException;
import com.app.wallet.exception.UserDoesNotExistException;
import com.app.wallet.model.Role;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.repository.WalletRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterUserRequestDto request)
    {

            if(userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }

            User user = new User();

            user.setFirstName(request.getFirstName());

            user.setLastName(request.getLastName());

            user.setEmail(request.getEmail());

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );

            user.setRole(Role.USER.name());
        long userId;
        try {

            userId = userRepository.createUser(user);
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException(request.getEmail(), ex);
        }
        walletRepository.createWallet(userId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> listUsers(int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<UserResponseDto> content = userRepository
                .findAll(safeSize, safePage * safeSize)
                .stream()
                .map(UserResponseDto::from)
                .toList();

        return new PageResponseDto<>(content, safePage, safeSize, userRepository.count());
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long id) {
        return UserResponseDto.from(findOrThrow(id));
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequestDto request) {

        User user = findOrThrow(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        try {
            userRepository.updateProfile(id, request.firstName(), request.lastName(), request.email());
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException(request.email(), ex);
        }

        return UserResponseDto.from(findOrThrow(id));
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequestDto request) {

        User user = findOrThrow(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        userRepository.updatePassword(id, passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public UserResponseDto changeRole(User actingUser, Long id, Role role) {

        if (actingUser.getId().equals(id)) {
            throw new ConflictException("You cannot change your own role");
        }

        findOrThrow(id);
        userRepository.updateRole(id, role.name());

        return UserResponseDto.from(findOrThrow(id));
    }

    @Transactional
    public UserResponseDto changeStatus(User actingUser, Long id, boolean active) {

        if (actingUser.getId().equals(id)) {
            throw new ConflictException("You cannot change your own status");
        }

        findOrThrow(id);
        userRepository.updateActive(id, active);

        return UserResponseDto.from(findOrThrow(id));
    }

    @Transactional
    public void deleteUser(User actingUser, Long id) {

        if (actingUser.getId().equals(id)) {
            throw new ConflictException("You cannot delete your own account");
        }

        findOrThrow(id);

        if (walletRepository.hasActivity(id)) {
            throw new ConflictException(
                    "User has a wallet balance or transaction history; deactivate the account instead");
        }

        userRepository.deleteById(id);
    }

    private User findOrThrow(Long id) {
        return userRepository.findUserById(id)
                .orElseThrow(() -> new UserDoesNotExistException(id));
    }
}
