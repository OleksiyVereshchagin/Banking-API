package com.learn.bankingapi.service;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {

        User user = userRepository.findUserByEmailIgnoreCase(login)
                .orElseGet(() -> userRepository.findUserByPhoneNumber(login)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login)));

        return new CustomUserDetails(user);
    }
}