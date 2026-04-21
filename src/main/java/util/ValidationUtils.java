package util;

import model.Major;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^(?=.{2,40}$)(?!.*(?:\\s{2}|['-]{2}))[\\p{L}]+(?:[ '-][\\p{L}]+)*$");

    private static final Pattern DEPARTMENT_PATTERN =
            Pattern.compile("^(?=.{2,60}$)(?!.*\\s{2})[A-Za-z][A-Za-z&\\- ]*[A-Za-z]$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^(?=.{6,254}$)(?=.{1,64}@)(?!\\.)(?!.*\\.\\.)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");

    private static final Pattern IMAGE_URL_PATTERN =
            Pattern.compile("^(?=.{0,300}$)(?:https?://(?:[\\w-]+\\.)+[\\w-]{2,}(?:/[^\\s]*)?|file:/\\S+)?$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^(?=.{5,30}$)(?![_.])(?!.*[_.]{2})[A-Za-z][A-Za-z0-9._]*(?<![_.])$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.{10,64}$)(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?])(?!.*\\s).*$");

    public static boolean isValidName(String value) {
        return value != null && NAME_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidDepartment(String value) {
        return value != null && DEPARTMENT_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidImageUrl(String value) {
        return value != null && IMAGE_URL_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidMajor(Major major) {
        return major != null;
    }

    public static boolean isValidUsername(String value) {
        return value != null && USERNAME_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isValidPassword(String value) {
        return value != null && PASSWORD_PATTERN.matcher(value).matches();
    }
}
