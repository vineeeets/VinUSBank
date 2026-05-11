package com.vinusbank.apigateway.filter;

import com.vinusbank.apigateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);


    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (validator.isSecured.test(exchange.getRequest())) {
                log.debug("[GATEWAY] Intercepting secured path: {}", path);

                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.warn("[GATEWAY] ✗ Blocked request to {} — Missing auth header", path);
                    return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7).trim(); 
                } else {
                    log.warn("[GATEWAY] ✗ Blocked request to {} — Invalid token format", path);
                    return onError(exchange, "Invalid Token format", HttpStatus.UNAUTHORIZED);
                }

                try {
                    jwtUtil.validateToken(authHeader);
                    String userEmail = jwtUtil.extractEmail(authHeader);
                    log.info("[GATEWAY] ✓ Token valid for user: {} | Routing to: {}", userEmail, path);

                    ServerHttpRequest request = exchange.getRequest()
                            .mutate()
                            .header("X-User-Email", userEmail)
                            .build();
                            
                    return chain.filter(exchange.mutate().request(request).build());

                } catch (Exception e) {
                    log.error("[GATEWAY] ✗ Token validation failed for path: {} | Reason: {}", path, e.getMessage());
                    return onError(exchange, "Unauthorized access to application", HttpStatus.UNAUTHORIZED);
                }
            }

            log.debug("[GATEWAY] Routing public path: {}", path);
            return chain.filter(exchange);
        });
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
    }
}

