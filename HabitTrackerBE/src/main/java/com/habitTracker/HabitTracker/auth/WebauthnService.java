package com.habitTracker.HabitTracker.auth;

import com.habitTracker.HabitTracker.auth.dto.LoginOptionsRequest;
import com.habitTracker.HabitTracker.auth.dto.LoginOptionsResponse;
import com.habitTracker.HabitTracker.auth.dto.LoginVerifyRequest;
import com.habitTracker.HabitTracker.auth.dto.LoginVerifyResponse;
import com.habitTracker.HabitTracker.auth.dto.RegistrationOptionsRequest;
import com.habitTracker.HabitTracker.auth.dto.RegistrationOptionsResponse;
import com.habitTracker.HabitTracker.auth.dto.RegistrationVerifyRequest;
import com.habitTracker.HabitTracker.user.User;
import com.habitTracker.HabitTracker.user.UserRepository;
import com.habitTracker.HabitTracker.user.dto.UserResponse;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebauthnService {

    private static final List<PublicKeyCredentialParameters> PUB_KEY_CRED_PARAMS = List.of(
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
    );
    private static final long TIMEOUT_MILLIS = 60_000;

    private final WebAuthnManager webAuthnManager;
    private final AttestedCredentialDataConverter attestedCredentialDataConverter;
    private final ChallengeStore challengeStore;
    private final UserRepository userRepository;
    private final WebauthnCredentialRepository credentialRepository;
    private final JwtService jwtService;

    @Value("${app.webauthn.rp-id}")
    private String rpId;

    @Value("${app.webauthn.rp-name}")
    private String rpName;

    @Value("${app.webauthn.origin}")
    private String originUrl;

    @Transactional(readOnly = true)
    public RegistrationOptionsResponse registrationOptions(RegistrationOptionsRequest request) {
        Challenge challenge = challengeStore.issue(request.email());
        String userHandle = Base64UrlUtil.encodeToString(userHandleFor(request.email()));

        return new RegistrationOptionsResponse(
                new RegistrationOptionsResponse.RelyingParty(rpId, rpName),
                new RegistrationOptionsResponse.UserEntity(userHandle, request.email(), request.displayName()),
                Base64UrlUtil.encodeToString(challenge.getValue()),
                PUB_KEY_CRED_PARAMS.stream()
                        .map(p -> new RegistrationOptionsResponse.PubKeyCredParam(p.getType().getValue(), (int) p.getAlg().getValue()))
                        .collect(Collectors.toList()),
                TIMEOUT_MILLIS,
                "none"
        );
    }

    @Transactional
    public UserResponse registrationVerify(RegistrationVerifyRequest request) {
        Challenge challenge = challengeStore.consume(request.email());

        Set<String> transports = request.transports() == null ? null : Set.copyOf(request.transports());
        RegistrationRequest registrationRequest = new RegistrationRequest(
                Base64UrlUtil.decode(request.attestationObject()),
                Base64UrlUtil.decode(request.clientDataJSON()),
                transports
        );
        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);

        ServerProperty serverProperty = new ServerProperty(new Origin(originUrl), rpId, challenge, null);
        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, PUB_KEY_CRED_PARAMS, false, true);
        webAuthnManager.verify(registrationData, registrationParameters);

        AttestedCredentialData attestedCredentialData = registrationData.getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        User user = userRepository.findByEmail(request.email()).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(request.email());
            newUser.setDisplayName(request.displayName());
            newUser.setDateOfBirth(request.dateOfBirth());
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            return newUser;
        });
        user = userRepository.save(user);

        WebauthnCredential credential = new WebauthnCredential();
        credential.setUser(user);
        credential.setCredentialId(Base64UrlUtil.encodeToString(attestedCredentialData.getCredentialId()));
        credential.setPublicKeyCose(Base64.getEncoder().encodeToString(attestedCredentialDataConverter.convert(attestedCredentialData)));
        credential.setSignCount(registrationData.getAttestationObject().getAuthenticatorData().getSignCount());
        credential.setAaguid(attestedCredentialData.getAaguid().toString());
        credential.setTransports(request.transports() == null ? null : String.join(",", request.transports()));
        credential.setCreatedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        return new UserResponse(
                user.getUserSeqId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getDateOfBirth() == null ? 0 : UserResponse.ageFrom(user.getDateOfBirth()),
                user.getWeightKg(),
                user.getHeightCm()
        );
    }

    @Transactional(readOnly = true)
    public LoginOptionsResponse loginOptions(LoginOptionsRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.email()));

        List<WebauthnCredential> credentials = credentialRepository.findByUser(user.getUserSeqId());
        List<LoginOptionsResponse.AllowCredential> allowCredentials = credentials.stream()
                .map(c -> new LoginOptionsResponse.AllowCredential("public-key", c.getCredentialId()))
                .collect(Collectors.toList());

        Challenge challenge = challengeStore.issue(request.email());

        return new LoginOptionsResponse(
                Base64UrlUtil.encodeToString(challenge.getValue()),
                TIMEOUT_MILLIS,
                rpId,
                "preferred",
                allowCredentials
        );
    }

    @Transactional
    public LoginVerifyResponse loginVerify(LoginVerifyRequest request) {
        Challenge challenge = challengeStore.consume(request.email());

        WebauthnCredential credential = credentialRepository.findByCredentialId(request.credentialId())
                .orElseThrow(() -> new EntityNotFoundException("Credential not found: " + request.credentialId()));
        User user = credential.getUser();
        if (!user.getEmail().equals(request.email())) {
            throw new IllegalArgumentException("Credential does not belong to email: " + request.email());
        }

        AttestedCredentialData attestedCredentialData = attestedCredentialDataConverter.convert(
                Base64.getDecoder().decode(credential.getPublicKeyCose()));

        CredentialRecordImpl credentialRecord = new CredentialRecordImpl(
                new NoneAttestationStatement(),
                null, null, null,
                credential.getSignCount(),
                attestedCredentialData,
                new AuthenticationExtensionsAuthenticatorOutputs<>(),
                null, null, null
        );

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                Base64UrlUtil.decode(request.credentialId()),
                request.userHandle() == null ? null : Base64UrlUtil.decode(request.userHandle()),
                Base64UrlUtil.decode(request.authenticatorData()),
                Base64UrlUtil.decode(request.clientDataJSON()),
                null,
                Base64UrlUtil.decode(request.signature())
        );

        ServerProperty serverProperty = new ServerProperty(new Origin(originUrl), rpId, challenge, null);
        AuthenticationParameters authenticationParameters = new AuthenticationParameters(serverProperty, credentialRecord, null, false);

        AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        webAuthnManager.verify(authenticationData, authenticationParameters);

        credential.setSignCount(authenticationData.getAuthenticatorData().getSignCount());

        String token = jwtService.issue(user.getUserSeqId(), user.getEmail());
        return new LoginVerifyResponse(token, user.getUserSeqId(), user.getEmail());
    }

    private byte[] userHandleFor(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(email.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
