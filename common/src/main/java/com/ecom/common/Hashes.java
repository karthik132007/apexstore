package com.ecom.common;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
public final class Hashes {
    private Hashes() {}
    public static String sha256(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
