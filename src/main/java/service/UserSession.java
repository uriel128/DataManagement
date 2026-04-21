package service;

import java.util.Objects;
import java.util.prefs.Preferences;

public final class UserSession {
    private static final Object LOCK = new Object();
    private static volatile UserSession instance;

    private static final Preferences APP_PREFS =
            Preferences.userNodeForPackage(UserSession.class).node("app");
    private static final Preferences USER_PREFS =
            Preferences.userNodeForPackage(UserSession.class).node("users");

    private final String userName;
    private final String password;
    private final String privileges;

    private UserSession(String userName, String password, String privileges) {
        this.userName = userName;
        this.password = password;
        this.privileges = privileges;
    }

    public static UserSession createSession(String userName, String password, String privileges) {
        Objects.requireNonNull(userName, "userName");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(privileges, "privileges");

        synchronized (LOCK) {
            instance = new UserSession(userName, password, privileges);
        }

        recordSignIn(userName, password, privileges);
        return instance;
    }

    public static UserSession getCurrentSession() {
        return instance;
    }

    public static void clearSession() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    public static synchronized boolean registerUser(String userName, String password, String privileges) {
        if (userExists(userName)) {
            return false;
        }
        USER_PREFS.put(userName.trim(), password + "::" + privileges);
        return true;
    }

    public static synchronized boolean userExists(String userName) {
        return USER_PREFS.get(userName == null ? "" : userName.trim(), null) != null;
    }

    public static synchronized String authenticate(String userName, String password) {
        String key = userName == null ? "" : userName.trim();
        String record = USER_PREFS.get(key, null);
        if (record == null) {
            return null;
        }

        String[] parts = record.split("::", 2);
        if (parts.length < 2) {
            return null;
        }

        if (!Objects.equals(parts[0], password)) {
            return null;
        }
        return parts[1];
    }

    private static synchronized void recordSignIn(String userName, String password, String privileges) {
        APP_PREFS.put("USERNAME", userName);
        APP_PREFS.put("PASSWORD", password);
        APP_PREFS.put("PRIVILEGES", privileges);
    }

    public static String getLastSignedInUsername() {
        return APP_PREFS.get("USERNAME", "");
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getPrivileges() {
        return privileges;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "userName='" + userName + '\'' +
                ", privileges='" + privileges + '\'' +
                '}';
    }
}
