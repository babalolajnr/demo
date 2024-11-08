package com.hello_world.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hello_world.demo.model.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email
     * 
     * @param email the email to search for
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     * 
     * @param email the email to check
     * @return boolean
     */
    boolean existsByEmail(String email);
}