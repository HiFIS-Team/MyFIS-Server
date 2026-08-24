package com.myfis.server.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.myfis.server.auth.AuthDtos.LoginRequest;
import com.myfis.server.auth.AuthDtos.PhoneVerificationConfirm;
import com.myfis.server.auth.AuthDtos.PhoneVerificationRequest;
import com.myfis.server.auth.AuthDtos.SignupRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/phone/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.MessageResponse requestPhone(@Valid @RequestBody PhoneVerificationRequest request) {
        authService.requestPhoneVerification(request.phoneNumber());
        return new AuthDtos.MessageResponse("Verification code requested");
    }

    @PostMapping("/phone/confirm")
    public AuthDtos.MessageResponse confirmPhone(@Valid @RequestBody PhoneVerificationConfirm request) {
        authService.confirmPhoneVerification(request.phoneNumber(), request.code());
        return new AuthDtos.MessageResponse("Phone number verified");
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}