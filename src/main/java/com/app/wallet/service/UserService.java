package com.app.wallet.service;

import com.app.wallet.dto.RegisterUserRequestDto;
import com.app.wallet.exception.EmailAlreadyExistsException;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterUserRequestDto request)
    {

            if(repository.existsByEmail(request.getEmail())) {
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
        try {
            repository.save(user);
        } catch (DuplicateKeyException ex) {
            throw new EmailAlreadyExistsException(request.getEmail(), ex);
        }
    }

}