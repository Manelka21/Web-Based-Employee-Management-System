package com.lankatech.ems.util;

/**
 * Regular expressions shared by the request DTOs (@Pattern needs compile-time constants).
 * The frontend (js/core/validators.js) uses the same rules, so both sides agree.
 */
public final class ValidationRules {

    private ValidationRules() {
    }

    // Letters (any language, with combining marks for Sinhala/Tamil vowel signs),
    // spaces, dots, apostrophes and hyphens; must start with a letter
    public static final String NAME = "^\\p{L}[\\p{L}\\p{M} .'-]*$";
    public static final String NAME_MESSAGE = "may only contain letters, spaces, dots, apostrophes and hyphens";

    // Sri Lankan phone: 0XXXXXXXXX or +94XXXXXXXXX. Empty is allowed (field is optional).
    public static final String PHONE = "^$|^(?:0\\d{9}|\\+94\\d{9})$";
    public static final String PHONE_MESSAGE = "must be a Sri Lankan number: 10 digits starting with 0, or +94 followed by 9 digits";

    // Sri Lankan NIC: old format 9 digits + V/X, or new format 12 digits
    public static final String NIC = "^(?:\\d{9}[VvXx]|\\d{12})$";
    public static final String NIC_MESSAGE = "must be 9 digits followed by V or X, or 12 digits";

    // Email with a domain that has a dot (plain @Email accepts "a@b")
    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";
    public static final String EMAIL_MESSAGE = "must be a valid email address such as name@company.lk";

    // 8–72 characters (BCrypt only uses the first 72 bytes) with at least one letter and one digit
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE = "must be 8–72 characters and contain at least one letter and one number";

    // ---------- normalisers used by the services ----------

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    // "077 123-4567" -> "0771234567"; blank -> null
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String cleaned = phone.replaceAll("[\\s-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    // "123456789v" -> "123456789V"
    public static String normalizeNic(String nic) {
        return nic == null ? null : nic.trim().toUpperCase();
    }
}
