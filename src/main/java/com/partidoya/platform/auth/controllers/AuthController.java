package com.partidoya.platform.auth.controllers;

import com.partidoya.platform.auth.services.AuthService;
import com.partidoya.platform.auth.security.JwtTokenService;
import com.partidoya.platform.auth.dto.AuthenticatedUserResource;
import com.partidoya.platform.auth.dto.LoginUserResource;
import com.partidoya.platform.auth.dto.RegisterUserResource;
import com.partidoya.platform.users.dto.UserResource;
import com.partidoya.platform.auth.dto.AuthenticateUserCommandFromResourceAssembler;
import com.partidoya.platform.auth.dto.RegisterUserCommandFromResourceAssembler;
import com.partidoya.platform.users.dto.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResource> register(@RequestBody RegisterUserResource resource) {
        var command = RegisterUserCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = authService.register(command);
        var body = UserResourceFromEntityAssembler.toResourceFromEntity(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResource> login(@RequestBody LoginUserResource resource) {
        var command = AuthenticateUserCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = authService.authenticate(command);
        var token = jwtTokenService.generateToken(user.getId().value(), user.getEmail().value());
        var body = new AuthenticatedUserResource(
                token,
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getRole().name(),
                user.getPlan().name());
        return ResponseEntity.ok(body);
    }
}
