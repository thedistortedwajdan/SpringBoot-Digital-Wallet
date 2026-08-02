package com.app.wallet.service;

import com.app.wallet.dto.LoginUserRequestDto;
import com.app.wallet.dto.LoginUserResponseDto;
import com.app.wallet.exception.InvalidCredentialsException;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    public LoginUserResponseDto login(LoginUserRequestDto request)
    {
        log.info("inside login");
        Optional<User> optional = userRepository.findUserByEmail(request.getEmail());

        User user = optional.orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException();
        }



        LoginUserResponseDto response = new LoginUserResponseDto();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setAccessToken(jwtService.generateToken(user.getId()));

        return response;


    }
}
