package com.habitTracker.HabitTracker.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebauthnCredentialRepository extends JpaRepository<WebauthnCredential, Long> {

    @Query("SELECT c FROM WebauthnCredential c WHERE c.credentialId = :credentialId")
    Optional<WebauthnCredential> findByCredentialId(@Param("credentialId") String credentialId);

    @Query("SELECT c FROM WebauthnCredential c WHERE c.user.userSeqId = :userSeqId")
    List<WebauthnCredential> findByUser(@Param("userSeqId") Long userSeqId);
}
