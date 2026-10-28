package com.example.gateway;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class BffController {

    @GetMapping("/bff/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user) {
        return Mono.just(Map.of(
                "sub", user.getSubject(),
                "name", user.getFullName(),
                "email", user.getEmail()
        ));
    }

    @GetMapping("/bff/csrf")
    public Mono<CsrfToken> csrf(Mono<CsrfToken> token) {
        return token;
    }
}
