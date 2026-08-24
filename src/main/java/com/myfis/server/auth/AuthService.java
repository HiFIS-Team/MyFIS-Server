package com.myfis.server.auth;

import java.util.Locale;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.myfis.server.auth.AuthDtos.LoginRequest;
import com.myfis.server.auth.AuthDtos.SignupRequest;
import com.myfis.server.auth.AuthDtos.RefreshRequest;

@Service
public class AuthService {
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final long refreshTokenDays;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PhoneVerificationService phoneVerificationService, JwtService jwtService,
                       StringRedisTemplate redis,
                       @Value("${app.jwt.refresh-token-days:14}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneVerificationService = phoneVerificationService;
        this.jwtService = jwtService;
        this.redis = redis;
        this.refreshTokenDays = refreshTokenDays;
    }

    public void requestPhoneVerification(String phoneNumber) {
        phoneVerificationService.request(phoneNumber);
    }

    public void confirmPhoneVerification(String phoneNumber, String code) {
        phoneVerificationService.confirm(phoneNumber, code);
    }

    @Transactional
    public AuthDtos.TokenResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = normalizePhone(request.phoneNumber());
        if (!phoneVerificationService.consume(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone verification is required");
        }
        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByPhoneNumber(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone number is already registered");
        }
        User user = userRepository.save(new User(request.name().trim(), email,
            passwordEncoder.encode(request.password()), request.age(), request.gender().trim(), phone,
            request.height(), request.weight(), request.exerciseExperience().trim(), request.referrer()));
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = identifier.contains("@")
            ? userRepository.findByEmailIgnoreCase(identifier).orElse(null)
            : userRepository.findByPhoneNumber(normalizePhone(identifier)).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    public AuthDtos.TokenResponse refresh(RefreshRequest request) {
        var claims = jwtService.parseRefresh(request.refreshToken());
        String refreshKey = REFRESH_PREFIX + claims.getId();
        String userId = redis.opsForValue().getAndDelete(refreshKey);
        if (userId == null || !userId.equals(claims.getSubject())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or already used");
        }
        User user = userRepository.findById(Long.valueOf(userId)).filter(User::isActive).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is unavailable"));
        return issueTokens(user);
    }

    public void logout(RefreshRequest request) {
        try {
            var claims = jwtService.parseRefresh(request.refreshToken());
            redis.delete(REFRESH_PREFIX + claims.getId());
        } catch (RuntimeException ignored) {
            // Logout is idempotent even when the client already discarded the token.
        }
    }

    private AuthDtos.TokenResponse issueTokens(User user) {
        String refreshToken = jwtService.issueRefresh(user);
        var refreshClaims = jwtService.parseRefresh(refreshToken);
        redis.opsForValue().set(REFRESH_PREFIX + refreshClaims.getId(), user.getId().toString(),
            Duration.ofDays(refreshTokenDays));
        return new AuthDtos.TokenResponse(jwtService.issueAccess(user), refreshToken, "Bearer",
            jwtService.accessTokenLifetimeSeconds());
    }

    private String normalizePhone(String phoneNumber) {
        String phone = phoneNumber.replaceAll("[ -]", "");
        if (!phone.matches("^\\+?[0-9]{9,15}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number");
        }
        return phone;
    }
}