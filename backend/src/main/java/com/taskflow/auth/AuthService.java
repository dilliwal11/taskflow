package com.taskflow.auth;

import com.taskflow.auth.AuthDtos.AuthResponse;
import com.taskflow.auth.AuthDtos.LoginRequest;
import com.taskflow.auth.AuthDtos.RegisterRequest;
import com.taskflow.common.Exceptions.ConflictException;
import com.taskflow.user.User;
import com.taskflow.user.UserDto;
import com.taskflow.user.UserRepository;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        // Two simultaneous sign-ups can both pass the check above. The unique constraint on
        // users.email is the real guard: the loser gets a 409 from GlobalExceptionHandler.
        User user = users.save(new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return new AuthResponse(tokens.issue(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same message for "no such user" and "wrong password", so attackers cannot probe for accounts.
        User user = users.findByEmail(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(tokens.issue(user), UserDto.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
