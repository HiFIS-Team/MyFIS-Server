package com.myfis.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DummyPhoneVerificationProvider implements PhoneVerificationProvider {
    private final String dummyCode;

    public DummyPhoneVerificationProvider(@Value("${app.phone-auth.dummy-code}") String dummyCode) {
        this.dummyCode = dummyCode;
    }

    @Override
    public void send(String phoneNumber) {
        // Deliberately no SMS integration in local development.
    }

    @Override
    public boolean matches(String phoneNumber, String code) {
        return dummyCode.equals(code);
    }
}