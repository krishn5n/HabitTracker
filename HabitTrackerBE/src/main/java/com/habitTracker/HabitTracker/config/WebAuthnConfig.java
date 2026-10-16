package com.habitTracker.HabitTracker.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfig {

    @Bean
    public ObjectConverter objectConverter() {
        return new ObjectConverter();
    }

    @Bean
    public AttestedCredentialDataConverter attestedCredentialDataConverter(ObjectConverter objectConverter) {
        return new AttestedCredentialDataConverter(objectConverter);
    }

    /**
     * Non-strict manager: skips attestation-statement trust-chain verification (no MDS lookup).
     * Fine here since we request attestation "none" and don't care which authenticator model was used.
     */
    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }
}
