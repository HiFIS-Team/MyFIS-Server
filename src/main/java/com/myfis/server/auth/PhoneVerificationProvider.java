package com.myfis.server.auth;

public interface PhoneVerificationProvider {
    void send(String phoneNumber);
    boolean matches(String phoneNumber, String code);
}