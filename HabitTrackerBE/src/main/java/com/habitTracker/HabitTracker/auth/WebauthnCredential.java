package com.habitTracker.HabitTracker.auth;

import com.habitTracker.HabitTracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "webauthn_credential")
@Getter
@Setter
@NoArgsConstructor
public class WebauthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_seq_id")
    private Long credentialSeqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq_id", nullable = false)
    private User user;

    @Column(name = "credential_id", length = 512, nullable = false, unique = true)
    private String credentialId;

    @Column(name = "public_key_cose", length = 2048, nullable = false)
    private String publicKeyCose;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "aaguid", length = 64)
    private String aaguid;

    @Column(name = "transports", length = 100)
    private String transports;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
