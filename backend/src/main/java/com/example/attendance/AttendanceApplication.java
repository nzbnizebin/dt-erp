package com.example.attendance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AttendanceApplication {
    private AttendanceApplication() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path dataDir = root.resolve("../data").normalize();
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        Path databaseFile = dataDir.resolve("attendance.db");
        Path secretFile = dataDir.resolve("backend-secret.key");

        Database database = new Database(databaseFile);
        database.initialize();

        TokenService tokenService = TokenService.load(secretFile);
        PasswordHasher hasher = new PasswordHasher();
        AuthService authService = new AuthService(database, hasher, tokenService);
        EmployeeService employeeService = new EmployeeService(database);
        LeaveRequestService leaveService = new LeaveRequestService(database, employeeService);

        HttpServerRunner server = new HttpServerRunner(8080, authService, employeeService, leaveService);
        System.out.println("Attendance backend started on http://localhost:8080");
        server.start();
    }
}
