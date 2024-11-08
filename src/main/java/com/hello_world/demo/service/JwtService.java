package com.hello_world.demo.service;

import com.hello_world.demo.model.entity.User;

public interface JwtService {
    String generateToken(User user);
}