package com.app.wallet.service;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.dto.PageResponseDto;
import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.dto.UserResponseDto;
import com.app.wallet.exception.EmailAlreadyExistsException;
import com.app.wallet.exception.InvalidCredentialsException;
import com.app.wallet.model.Role;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.repository.WalletRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
}
