package com.abhay.expensetracker.security;

/**
 * The "who is calling" object. The JWT filter puts it into the SecurityContext and controllers
 * receive it with @AuthenticationPrincipal.
 */
public record AuthenticatedUser(Long id, String email) {
}
