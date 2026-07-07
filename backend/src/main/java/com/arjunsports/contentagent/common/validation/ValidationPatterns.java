package com.arjunsports.contentagent.common.validation;

public final class ValidationPatterns {

    /** Optional leading +, 8-15 digits total (E.164-ish). */
    public static final String PHONE = "^\\+?[1-9]\\d{7,14}$";

    /** At least 8 characters, at least one letter and one digit. */
    public static final String PASSWORD_STRENGTH = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    public static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters and include at least one letter and one digit";

    public static final String PHONE_MESSAGE = "Phone number must be a valid number (8-15 digits, optional +)";

    private ValidationPatterns() {
    }
}
