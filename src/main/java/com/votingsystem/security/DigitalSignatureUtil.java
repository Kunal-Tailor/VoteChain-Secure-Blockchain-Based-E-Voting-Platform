package com.votingsystem.security;

import java.security.*;
import java.util.Base64;

public class DigitalSignatureUtil {

    // Generate RSA Key Pair
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Sign Data
    public static String signData(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes());

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Verify Signature
    public static boolean verifySignature(String data, String signatureStr, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());

            byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            return false;
        }
    }
}
