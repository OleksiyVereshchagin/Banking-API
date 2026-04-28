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


    /**
     * Loads a user by their login identifier (email or phone number).
     *
     * Logic flow:
     * 1. Attempt to find the user by email (case-insensitive).
     * 2. If not found, attempt to find the user by phone number.
     * 3. If still not found, throw a {@link UsernameNotFoundException}.
     *
     * @param login the identifier used for authentication (can be email or phone number)
     * @return a {@link CustomUserDetails} object containing the authenticated user's data
     * @throws UsernameNotFoundException if no user is found with the provided email or phone number
     */
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {

        User user = userRepository.findUserByEmailIgnoreCase(login)
                .orElseGet(() -> userRepository.findUserByPhoneNumber(login)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login)));

        return new CustomUserDetails(user);
    }
}