package com.abhay.expensetracker.auth;

import com.abhay.expensetracker.auth.dto.AuthResponse;
import com.abhay.expensetracker.auth.dto.LoginRequest;
import com.abhay.expensetracker.auth.dto.RegisterRequest;
import com.abhay.expensetracker.common.ConflictException;
import com.abhay.expensetracker.security.JwtService;
import com.abhay.expensetracker.user.User;
import com.abhay.expensetracker.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' is already registered");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same message for "no such user" and "wrong password" so attackers can't probe which emails exist.
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail()),
                "Bearer", jwtService.expiresInSeconds(), user.getEmail());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
