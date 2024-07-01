package com.prpa.Shortz.model.shortener;

import com.prpa.Shortz.model.shortener.contract.StringEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class DigestToBase64StringEncoder implements StringEncoder {

    public static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";
    private final MessageDigest messageDigest;

    public DigestToBase64StringEncoder() {
        try {
            this.messageDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Não foi possível obter " + DEFAULT_DIGEST_ALGORITHM + " digest.");
        }
    }

    public DigestToBase64StringEncoder(MessageDigest digest) {
        this.messageDigest = digest;
    }

    @Override
    public String encode(String value) {
        byte[] hash = messageDigest.digest(value.getBytes());
        return Base64.getEncoder().encodeToString(hash).substring(1);
    }
}
