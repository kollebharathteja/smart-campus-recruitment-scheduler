package com.smartcampus.security;

/** Lightweight principal placed into the SecurityContext after JWT validation. */
public record AuthenticatedUser(String userId, String email, String role) {
}
