package com.medical.medcore.shared.security;

import java.security.SecureRandom;

/**
 * Códigos numéricos de un solo uso (OTP) que se envían por correo. Se guardan como hash ligado a un dueño (usuario o
 * correo) para que el mismo número en dos cuentas no produzca el mismo hash.
 */
public final class OneTimeCode {

    public static final int LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BOUND = 1_000_000;

    private OneTimeCode() {
    }

    public static String generate() {
        return String.format("%0" + LENGTH + "d", RANDOM.nextInt(BOUND));
    }

    public static String hash(String owner, String code) {
        return TokenHasher.sha256(owner + ":" + normalize(code));
    }

    public static String normalize(String code) {
        return code == null ? "" : code.replaceAll("\\D", "");
    }
}
