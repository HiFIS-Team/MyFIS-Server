package com.myfis.server.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record PhoneVerificationRequest(@NotBlank String phoneNumber) {}
    public record PhoneVerificationConfirm(@NotBlank String phoneNumber, @NotBlank String code) {}
    public record SignupRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull @Min(14) Integer age,
        @NotBlank String gender,
        @NotBlank String phoneNumber,
        @NotNull @Min(80) Integer height,
        @NotNull @Min(20) Integer weight,
        @NotBlank String exerciseExperience,
        String referrer) {}
    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType) {}
    public record MessageResponse(String message) {}
}