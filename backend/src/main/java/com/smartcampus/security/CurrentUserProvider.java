package com.smartcampus.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public AuthenticatedUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser au) {
            return au;
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    public String getCurrentUserId() {
        return getCurrentUser().userId();
    }
}
