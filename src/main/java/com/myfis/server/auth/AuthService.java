package com.myfis.server.auth;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.myfis.server.auth.AuthDtos.LoginRequest;
import com.myfis.server.auth.AuthDtos.SignupRequest;

@Service
public class AuthService {
    private static final String VERIFIED_PREFIX = "phone:verified:";
    private static final String REQUESTED_PREFIX = "phone:requested:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final JwtService jwtService;
    private final String dummyCode;
    private final long ttlSeconds;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       StringRedisTemplate redis, JwtService jwtService,
                       @Value("${app.phone-auth.dummy-code}") String dummyCode,
                       @Value("${app.phone-auth.ttl-seconds:300}") long ttlSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
        this.jwtService = jwtService;
        this.dummyCode = dummyCode;
        this.ttlSeconds = ttlSeconds;
    }

    public void requestPhoneVerification(String phoneNumber) {
        String phone = normalizePhone(phoneNumber);
        redis.opsForValue().set(REQUESTED_PREFIX + phone, "1", java.time.Duration.ofSeconds(ttlSeconds));
    }

    public void confirmPhoneVerification(String phoneNumber, String code) {
        String phone = normalizePhone(phoneNumber);
        if (!dummyCode.equals(code) || !Boolean.TRUE.equals(redis.hasKey(REQUESTED_PREFIX + phone))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone verification");
        }
        redis.opsForValue().set(VERIFIED_PREFIX + phone, "1", java.time.Duration.ofSeconds(ttlSeconds));
        redis.delete(REQUESTED_PREFIX + phone);
    }

    @Transactional
    public AuthDtos.TokenResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = normalizePhone(request.phoneNumber());
        if (!Boolean.TRUE.equals(redis.hasKey(VERIFIED_PREFIX + phone))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone verification is required");
        }
        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByPhoneNumber(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone number is already registered");
        }
        User user = userRepository.save(new User(request.name().trim(), email,
            passwordEncoder.encode(request.password()), request.age(), request.gender().trim(), phone,
            request.height(), request.weight(), request.exerciseExperience().trim(), request.referrer()));
        redis.delete(VERIFIED_PREFIX + phone);
        return new AuthDtos.TokenResponse(jwtService.issue(user), "Bearer");
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
        return new AuthDtos.TokenResponse(jwtService.issue(user), "Bearer");
    }

    private String normalizePhone(String phoneNumber) {
        String phone = phoneNumber.replaceAll("[^0-9+]", "");
        if (!phone.matches("^\\+?[0-9]{9,15}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number");
        }
        return phone;
    }
}