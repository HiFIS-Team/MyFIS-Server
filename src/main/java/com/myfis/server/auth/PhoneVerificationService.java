package com.myfis.server.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhoneVerificationService {
    private static final String REQUEST_PREFIX = "auth:phone:request:";
    private static final String VERIFIED_PREFIX = "auth:phone:verified:";
    private static final String ATTEMPT_PREFIX = "auth:phone:attempt:";
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final PhoneVerificationProvider provider;
    private final long ttlSeconds;

    public PhoneVerificationService(StringRedisTemplate redis,
                                    PhoneVerificationProvider provider,
                                    @Value("${app.phone-auth.ttl-seconds:300}") long ttlSeconds) {
        if (ttlSeconds < 60 || ttlSeconds > 900) {
            throw new IllegalArgumentException("Phone verification TTL must be between 60 and 900 seconds");
        }
        this.redis = redis;
        this.provider = provider;
        this.ttlSeconds = ttlSeconds;
    }

    public void request(String phoneNumber) {
        String phone = normalize(phoneNumber);
        provider.send(phone);
        Boolean created = redis.opsForValue().setIfAbsent(
            REQUEST_PREFIX + phone, "1", Duration.ofSeconds(ttlSeconds));
        if (!Boolean.TRUE.equals(created)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Please wait before requesting another code");
        }
    }

    public void confirm(String phoneNumber, String code) {
        String phone = normalize(phoneNumber);
        String requestKey = REQUEST_PREFIX + phone;
        if (!Boolean.TRUE.equals(redis.hasKey(requestKey))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request expired");
        }
        String attemptKey = ATTEMPT_PREFIX + phone;
        Long attempts = redis.opsForValue().increment(attemptKey);
        redis.expire(attemptKey, Duration.ofSeconds(ttlSeconds));
        if (attempts == null || attempts > MAX_ATTEMPTS) {
            redis.delete(requestKey);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts");
        }
        if (!provider.matches(phone, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }
        redis.opsForValue().set(VERIFIED_PREFIX + phone, "1", Duration.ofSeconds(ttlSeconds));
        redis.delete(requestKey);
        redis.delete(attemptKey);
    }

    public boolean consume(String phoneNumber) {
        return redis.opsForValue().getAndDelete(VERIFIED_PREFIX + normalize(phoneNumber)) != null;
    }

    private String normalize(String phoneNumber) {
        String phone = phoneNumber.replaceAll("[ -]", "");
        if (!phone.matches("^\\+?[0-9]{9,15}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number");
        }
        return phone;
    }
}