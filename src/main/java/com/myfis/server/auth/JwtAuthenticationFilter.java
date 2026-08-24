package com.myfis.server.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String subject = jwtService.parse(header.substring(7)).getSubject();
                var authentication = new UsernamePasswordAuthenticationToken(
                    subject, null, AuthorityUtils.NO_AUTHORITIES);
                org.springframework.security.core.context.SecurityContextHolder
                    .getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}