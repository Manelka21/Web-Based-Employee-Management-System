package com.lankatech.ems.util;

import java.util.Arrays;

public class EnumUtils {

    private EnumUtils() {
    }

    /**
     * Converts a String such as "approved" or "APPROVED" into an enum constant.
     * Throws IllegalArgumentException (-> HTTP 400) listing the allowed values
     * instead of Java's default "No enum constant ..." message.
     */
    public static <E extends Enum<E>> E parse(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required. Allowed values: "
                    + Arrays.toString(enumType.getEnumConstants()));
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "'. Allowed values: "
                    + Arrays.toString(enumType.getEnumConstants()));
        }
    }
}
