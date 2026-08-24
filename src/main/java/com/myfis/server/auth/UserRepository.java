package com.myfis.server.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
}