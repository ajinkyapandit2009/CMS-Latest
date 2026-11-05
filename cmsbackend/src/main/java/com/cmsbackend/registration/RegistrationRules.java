package com.cmsbackend.registration;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RegistrationRules {
    // Phase 6B must use the same rules: 8-72 non-whitespace characters, with upper/lowercase and a digit.
    public static final String PASSWORD_PATTERN = "^(?=\\S*[A-Z])(?=\\S*[a-z])(?=\\S*[0-9])\\S{8,72}$";
    public static final String EMAIL_PATTERN = "^(?!.*\\.\\.)[A-Za-z0-9_+%'-]+(?:\\.[A-Za-z0-9_+%'-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$";
    public static final String PHONE_PATTERN = "^(?=.*[0-9])[+0-9() .-]+$";
    private static final Pattern EMAIL = Pattern.compile(EMAIL_PATTERN);

    private RegistrationRules() { }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean validEmail(String email) {
        String normalized = normalizeEmail(email);
        return normalized != null && normalized.length() <= 254 && EMAIL.matcher(normalized).matches();
    }

    public static boolean fitsBcryptLimit(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public static boolean fitsAddressColumn(String address) {
        return address == null || address.getBytes(StandardCharsets.UTF_8).length <= 65535;
    }
}
