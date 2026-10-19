package org.example.lab1.service;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.example.lab1.dto.LoginRequest;
import org.example.lab1.dto.RegisterRequest;
import org.example.lab1.dto.TokenResponse;
import org.example.lab1.dto.UserResponse;
import org.example.lab1.model.User;
import org.example.lab1.repository.UserRepository;
import org.example.lab1.security.JwtService;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }

        User newUser = new User(username, passwordEncoder.encode(request.password()));
        try {
            return UserResponse.from(userRepository.saveAndFlush(newUser));
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint also protects against concurrent registrations.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
    }

    public TokenResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeUsername(request.username()), request.password()));
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            return new TokenResponse("Bearer", jwtService.generateToken(principal),
                    jwtService.getExpirationSeconds());
        } catch (AuthenticationException exception) {
            // Do not reveal whether the username or password was wrong.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    private String normalizeUsername(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
