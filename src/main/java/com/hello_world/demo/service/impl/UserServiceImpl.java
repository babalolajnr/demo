package com.hello_world.demo.service.impl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.hello_world.demo.model.dto.LoginRequest;
import com.hello_world.demo.model.dto.RegisterRequest;
import com.hello_world.demo.model.entity.User;
import com.hello_world.demo.repository.UserRepository;
import com.hello_world.demo.service.JwtService;
import com.hello_world.demo.service.UserService;
import com.hello_world.demo.exception.InvalidCredentialsException;
import com.hello_world.demo.exception.EntityAlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EntityAlreadyExistsException("User", "email", request.getEmail());
        }

        userRepository.save(User.builder().name(request.getName()).email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())).build());
    }

    @Override
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email/password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email/password");
        }

        return jwtService.generateToken(user);
    }
}
