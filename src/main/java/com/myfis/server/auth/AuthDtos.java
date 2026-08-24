package com.myfis.server.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {
    private AuthDtos() {}

    public record PhoneVerificationRequest(@NotBlank @Pattern(regexp = "\\+?[0-9][0-9 -]{8,18}") String phoneNumber) {}
    public record PhoneVerificationConfirm(
        @NotBlank @Pattern(regexp = "\\+?[0-9][0-9 -]{8,18}") String phoneNumber,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}
    public record SignupRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull @Min(14) Integer age,
        @NotBlank @Size(max = 30) String gender,
        @NotBlank @Pattern(regexp = "\\+?[0-9][0-9 -]{8,18}") String phoneNumber,
        @NotNull @Min(80) @jakarta.validation.constraints.Max(250) Integer height,
        @NotNull @Min(20) @jakarta.validation.constraints.Max(500) Integer weight,
        @NotBlank @Size(max = 50) String exerciseExperience,
        @Size(max = 80) String referrer) {}
    public record LoginRequest(@NotBlank @Size(max = 254) String identifier,
                               @NotBlank @Size(max = 100) String password) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresInSeconds) {}
    public record MessageResponse(String message) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
}