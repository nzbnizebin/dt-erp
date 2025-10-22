package com.example.attendance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AuthService {
    private static final long TOKEN_VALIDITY_SECONDS = Duration.ofHours(8).toSeconds();

    private final Database database;
    private final PasswordHasher hasher;
    private final TokenService tokenService;

    public AuthService(Database database, PasswordHasher hasher, TokenService tokenService) {
        this.database = Objects.requireNonNull(database, "database");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    }

    public Optional<LoginResult> login(String username, String password) {
        List<Map<String, String>> rows = database.query(
                "SELECT username, password_hash, salt, role FROM user_account WHERE username='" +
                        Database.escape(username) + "';");
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> row = rows.get(0);
        PasswordHasher.HashedPassword hashed = new PasswordHasher.HashedPassword(
                row.get("password_hash"), row.get("salt"));
        if (!hasher.verify(password, hashed)) {
            return Optional.empty();
        }
        String token = tokenService.generateToken(row.get("username"), row.get("role"), TOKEN_VALIDITY_SECONDS);
        return Optional.of(new LoginResult(token, row.get("role")));
    }

    public Optional<TokenService.TokenPayload> verify(String token) {
        return tokenService.verifyToken(token);
    }

    public record LoginResult(String token, String role) {
    }
}
