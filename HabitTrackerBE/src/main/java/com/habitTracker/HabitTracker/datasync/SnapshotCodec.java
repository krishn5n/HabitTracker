package com.habitTracker.HabitTracker.datasync;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzips and encrypts a whole snapshot with AES-256-GCM. The envelope that lands
 * in the data repository carries only the IV and the ciphertext, so nothing
 * about the contents is readable without the key from the environment.
 */
@Component
public class SnapshotCodec {

    private static final String ALG = "A256GCM";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();
    private final SecretKey key;

    public SnapshotCodec(ObjectMapper objectMapper, DataSyncProperties properties) {
        this.objectMapper = objectMapper;
        this.key = readKey(properties.getEncryptionKey());
    }

    public boolean hasKey() {
        return key != null;
    }

    /** Snapshot bytes to the envelope JSON stored in the data repository. */
    public byte[] seal(byte[] plaintext) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(gzip(plaintext));

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("v", 1);
        envelope.put("alg", ALG);
        envelope.put("compression", "gzip");
        envelope.put("iv", Base64.getEncoder().encodeToString(iv));
        envelope.put("ct", Base64.getEncoder().encodeToString(ciphertext));
        return objectMapper.writeValueAsBytes(envelope);
    }

    /** Envelope JSON back to the snapshot bytes. */
    public byte[] open(byte[] envelopeJson) throws Exception {
        JsonNode envelope = objectMapper.readTree(envelopeJson);
        String alg = envelope.path("alg").asText();
        if (!ALG.equals(alg)) {
            throw new IllegalStateException("Unsupported snapshot algorithm: " + alg);
        }

        byte[] iv = Base64.getDecoder().decode(envelope.path("iv").asText());
        byte[] ciphertext = Base64.getDecoder().decode(envelope.path("ct").asText());

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
        byte[] compressed = cipher.doFinal(ciphertext);

        return "gzip".equals(envelope.path("compression").asText()) ? gunzip(compressed) : compressed;
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY is not set; refusing to touch snapshots");
        }
        return key;
    }

    private static SecretKey readKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY must decode to 32 bytes, got " + raw.length);
        }
        return new SecretKeySpec(raw, "AES");
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(input);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] input) throws Exception {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(input))) {
            return gzip.readAllBytes();
        }
    }
}
