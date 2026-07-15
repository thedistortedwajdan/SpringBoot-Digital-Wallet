package com.app.wallet.service;

import com.app.wallet.dto.RegisterUserRequest;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public void register(RegisterUserRequest request)
    {
        Optional<User> existing =
                repository.findByEmail(request.getEmail());

        if(existing.isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();

        user.setFirstName(request.getFirstName());

        user.setLastName(request.getLastName());

        user.setEmail(request.getEmail());

        user.setPassword(request.getPassword());

        user.setRole("USER");

        repository.save(user);
    }

}