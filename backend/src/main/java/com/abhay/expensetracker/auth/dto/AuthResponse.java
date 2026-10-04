package com.abhay.expensetracker.auth.dto;

/** Send the token back as "Authorization: Bearer <token>" on every later request. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, String email) {
}
