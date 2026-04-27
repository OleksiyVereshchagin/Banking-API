package com.learn.bankingapi.repository;

import com.learn.bankingapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserById(Long id);

    Boolean existsUserByEmailIgnoreCase(String email);
    Boolean existsUserByPhoneNumber(String phoneNumber);
    Optional<User> findUserByEmailIgnoreCase(String email);
    Optional<User> findUserByPhoneNumber(String phoneNumber);
}
