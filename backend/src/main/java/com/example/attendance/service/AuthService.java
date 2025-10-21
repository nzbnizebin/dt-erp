package com.example.attendance.service;

import com.example.attendance.dto.LoginRequest;
import com.example.attendance.dto.LoginResponse;
import com.example.attendance.entity.UserAccount;
import com.example.attendance.repository.UserAccountRepository;
import com.example.attendance.security.JwtTokenService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @PostConstruct
    public void ensureDefaultAdmin() {
        Optional<UserAccount> admin = userAccountRepository.findByUsername("admin");
        if (admin.isEmpty()) {
            UserAccount account = new UserAccount();
            account.setUsername("admin");
            account.setPassword(passwordEncoder.encode("admin123"));
            account.setRole(com.example.attendance.entity.UserRole.ADMIN);
            userAccountRepository.save(account);
        }
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = jwtTokenService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name());
    }
}
