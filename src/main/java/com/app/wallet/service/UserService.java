package com.app.wallet.service;

import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.exception.EmailAlreadyExistsException;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.repository.WalletRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

            user.setRole("USER");
        long userId;
        try {

            userId = userRepository.createUser(user);
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException(request.getEmail(), ex);
        }
        walletRepository.createWallet(userId);
    }

}