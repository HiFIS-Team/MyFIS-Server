package com.myfis.server.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes:30}") long accessTokenMinutes,
                      @Value("${app.jwt.refresh-token-days:14}") long refreshTokenDays,
                      @Value("${app.jwt.issuer:myfis-server}") String issuer) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessTokenMinutes < 1 || accessTokenMinutes > 1440) {
            throw new IllegalArgumentException("JWT access token lifetime must be between 1 and 1440 minutes");
        }
        this.accessTokenMinutes = accessTokenMinutes;
        if (refreshTokenDays < 1 || refreshTokenDays > 90) {
            throw new IllegalArgumentException("JWT refresh token lifetime must be between 1 and 90 days");
        }
        this.refreshTokenDays = refreshTokenDays;
        this.issuer = issuer;
    }

    public String issueAccess(User user) {
        return issue(user, "access", accessTokenMinutes * 60);
    }

    public String issueRefresh(User user) {
        return issue(user, "refresh", refreshTokenDays * 86400);
    }

    public long accessTokenLifetimeSeconds() {
        return accessTokenMinutes * 60;
    }

    private String issue(User user, String tokenType, long lifetimeSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("token_type", tokenType)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(lifetimeSeconds)))
            .signWith(signingKey)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).requireIssuer(issuer).build()
            .parseSignedClaims(token).getPayload();
    }

    public Claims parseAccess(String token) {
        return requireType(parse(token), "access");
    }

    public Claims parseRefresh(String token) {
        return requireType(parse(token), "refresh");
    }

    private Claims requireType(Claims claims, String expectedType) {
        if (!expectedType.equals(claims.get("token_type", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }
        return claims;
    }
}