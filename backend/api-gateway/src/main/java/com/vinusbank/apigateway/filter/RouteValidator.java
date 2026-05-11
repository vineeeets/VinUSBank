package com.vinusbank.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // List of endpoints that are allowed to be accessed without a JWT token (Public Endpoints)
    public static final List<String> openApiEndpoints = List.of(
            "/auth-service/api/auth/register",
            "/auth-service/api/auth/login",
            "/eureka",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/swagger-resources",
            "/webjars"
    );

    // Checks if the incoming request path is one of the open endpoints
    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

}
