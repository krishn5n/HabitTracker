package com.habitTracker.HabitTracker.auth.dto;

import java.util.List;

public record RegistrationOptionsResponse(
        RelyingParty rp,
        UserEntity user,
        String challenge,
        List<PubKeyCredParam> pubKeyCredParams,
        long timeout,
        String attestation,
        AuthenticatorSelection authenticatorSelection
) {
    public record AuthenticatorSelection(String residentKey, boolean requireResidentKey, String userVerification) {}

    public record RelyingParty(String id, String name) {}

    public record UserEntity(String id, String name, String displayName) {}

    public record PubKeyCredParam(String type, int alg) {}
}
