package com.example.attendance;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public final class HttpServerRunner {
    private final int port;
    private final AuthService authService;
    private final EmployeeService employeeService;
    private final LeaveRequestService leaveService;
    private final List<Route> routes;

    public HttpServerRunner(int port, AuthService authService, EmployeeService employeeService,
                             LeaveRequestService leaveService) {
        this.port = port;
        this.authService = authService;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.routes = createRoutes();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RoutingHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private List<Route> createRoutes() {
        List<Route> list = new ArrayList<>();
        list.add(new Route("POST", "/api/auth/login", false, null, (body, params, query, user) -> {
            String username = body.getOrDefault("username", "").toString();
            String password = body.getOrDefault("password", "").toString();
            return authService.login(username, password)
                    .map(result -> Map.of("token", result.token(), "role", result.role()))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        }));
        list.add(new Route("GET", "/api/employees", true, null, (body, params, query, user) -> {
            List<Map<String, Object>> result = new ArrayList<>();
            for (EmployeeService.Employee employee : employeeService.listEmployees()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", employee.id());
                item.put("chineseName", employee.chineseName());
                item.put("englishName", employee.englishName());
                item.put("hireDate", employee.hireDate().toString());
                result.add(item);
            }
            return result;
        }));
        list.add(new Route("POST", "/api/employees", true, "ADMIN", (body, params, query, user) -> {
            String chineseName = body.getOrDefault("chineseName", "").toString();
            String englishName = body.getOrDefault("englishName", "").toString();
            String hireDate = body.getOrDefault("hireDate", "").toString();
            EmployeeService.Employee employee = employeeService
                    .createEmployee(chineseName, englishName, LocalDate.parse(hireDate));
            return Map.of(
                    "id", employee.id(),
                    "chineseName", employee.chineseName(),
                    "englishName", employee.englishName(),
                    "hireDate", employee.hireDate().toString()
            );
        }));
        list.add(new Route("PUT", "/api/employees/{id}", true, "ADMIN", (body, params, query, user) -> {
            long id = Long.parseLong(params.get("id"));
            String chineseName = body.getOrDefault("chineseName", "").toString();
            String englishName = body.getOrDefault("englishName", "").toString();
            String hireDate = body.getOrDefault("hireDate", "").toString();
            EmployeeService.Employee employee = employeeService
                    .updateEmployee(id, chineseName, englishName, LocalDate.parse(hireDate));
            return Map.of(
                    "id", employee.id(),
                    "chineseName", employee.chineseName(),
                    "englishName", employee.englishName(),
                    "hireDate", employee.hireDate().toString()
            );
        }));
        list.add(new Route("DELETE", "/api/employees/{id}", true, "ADMIN", (body, params, query, user) -> {
            long id = Long.parseLong(params.get("id"));
            employeeService.deleteEmployee(id);
            return Map.of("status", "deleted");
        }));
        list.add(new Route("GET", "/api/employees/{id}/annual-leave", true, null, (body, params, query, user) -> {
            long id = Long.parseLong(params.get("id"));
            EmployeeService.AnnualLeaveSummary summary = employeeService.calculateAnnualLeave(id);
            return Map.of(
                    "totalQuota", summary.totalQuota(),
                    "usedDays", summary.usedDays(),
                    "remainingDays", summary.remainingDays()
            );
        }));
        list.add(new Route("POST", "/api/leave-requests", true, null, (body, params, query, user) -> {
            LeaveRequestService.LeaveRequest request = leaveService.createLeaveRequest(body);
            return Map.of(
                    "id", request.id(),
                    "englishName", request.employee().englishName(),
                    "type", request.type().name(),
                    "startTime", request.startTime().toString(),
                    "endTime", request.endTime().toString(),
                    "hours", request.hours()
            );
        }));
        list.add(new Route("GET", "/api/leave-requests", true, null, (body, params, query, user) -> {
            LeaveRequestService.PagedResult result = leaveService.listLeaveRequests(query);
            return Map.of(
                    "content", result.items(),
                    "page", result.page(),
                    "size", result.size(),
                    "totalElements", result.total()
            );
        }));
        list.add(new Route("DELETE", "/api/leave-requests/{id}", true, "ADMIN", (body, params, query, user) -> {
            long id = Long.parseLong(params.get("id"));
            leaveService.deleteLeaveRequest(id);
            return Map.of("status", "deleted");
        }));
        return List.copyOf(list);
    }

    private final class RoutingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    handleOptions(exchange);
                    return;
                }
                Route route = findRoute(method, path);
                if (route == null) {
                    sendJson(exchange, 404, Map.of("message", "Not found"));
                    return;
                }
                AuthenticatedUser user = null;
                if (route.requiresAuth) {
                    user = authenticate(exchange.getRequestHeaders());
                    if (user == null) {
                        sendJson(exchange, 401, Map.of("message", "Unauthorized"));
                        return;
                    }
                    if (route.requiredRole != null && !route.requiredRole.equals(user.role())) {
                        sendJson(exchange, 403, Map.of("message", "Forbidden"));
                        return;
                    }
                }
                Map<String, String> pathParams = route.extractParams(path);
                Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> jsonBody = body.isBlank() ? Map.of() : Json.parseObject(body);
                Object response = route.handler.handle(jsonBody, pathParams, queryParams, user);
                sendJson(exchange, 200, response);
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, Map.of("message", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, Map.of("message", "Internal server error"));
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            exchange.sendResponseHeaders(204, -1);
        }

        private AuthenticatedUser authenticate(Headers headers) {
            String authHeader = headers.getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.substring(7);
            Optional<TokenService.TokenPayload> payload = authService.verify(token);
            return payload.map(p -> new AuthenticatedUser(p.username(), p.role())).orElse(null);
        }

        private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
            byte[] bytes = Json.toJson(payload).getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/json; charset=utf-8");
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String rawQuery) {
            Map<String, String> params = new HashMap<>();
            if (rawQuery == null || rawQuery.isBlank()) {
                return params;
            }
            for (String pair : rawQuery.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int idx = pair.indexOf('=');
                if (idx >= 0) {
                    String key = decode(pair.substring(0, idx));
                    String value = decode(pair.substring(idx + 1));
                    params.put(key, value);
                } else {
                    params.put(decode(pair), "");
                }
            }
            return params;
        }

        private String decode(String value) {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private Route findRoute(String method, String path) {
            for (Route route : routes) {
                if (route.matches(method, path)) {
                    return route;
                }
            }
            return null;
        }
    }

    private record AuthenticatedUser(String username, String role) {
    }

    private interface RouteHandler {
        Object handle(Map<String, Object> body, Map<String, String> pathParams,
                       Map<String, String> queryParams, AuthenticatedUser user);
    }

    private static final class Route {
        private final String method;
        private final String pathPattern;
        private final boolean requiresAuth;
        private final String requiredRole;
        private final RouteHandler handler;
        private final String[] segments;

        Route(String method, String pathPattern, boolean requiresAuth, String requiredRole, RouteHandler handler) {
            this.method = method;
            this.pathPattern = pathPattern;
            this.requiresAuth = requiresAuth;
            this.requiredRole = requiredRole;
            this.handler = handler;
            this.segments = pathPattern.split("/");
        }

        boolean matches(String requestMethod, String requestPath) {
            if (!method.equalsIgnoreCase(requestMethod)) {
                return false;
            }
            String[] pathSegments = requestPath.split("/");
            if (pathSegments.length != segments.length) {
                return false;
            }
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("{")) {
                    continue;
                }
                if (!segments[i].equals(pathSegments[i])) {
                    return false;
                }
            }
            return true;
        }

        Map<String, String> extractParams(String path) {
            String[] pathSegments = path.split("/");
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].startsWith("{")) {
                    String key = segments[i].substring(1, segments[i].length() - 1);
                    params.put(key, pathSegments[i]);
                }
            }
            return params;
        }
    }
}
