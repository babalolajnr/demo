package com.hello_world.demo.service;

import com.hello_world.demo.model.dto.LoginRequest;
import com.hello_world.demo.model.dto.RegisterRequest;

public interface UserService {
    /**
     * Registers a new user in the system.
     *
     * @param request the registration request containing user details
     * @throws IllegalArgumentException   if the request is null or contains invalid
     *                                    data
     * @throws UserAlreadyExistsException if a user with the same email/username
     *                                    already exists
     * @since 1.0
     */
    void register(RegisterRequest request);

    String login(LoginRequest request);
}