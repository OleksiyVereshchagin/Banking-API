package com.learn.bankingapi.utils;

import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Helper component for retrieving the currently authenticated user from the Security Context.
 */
@Component
public class CurrentUserProvider {
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails user)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated or session has expired"
            );
        }

        return user.getUser();
    }
}
