package model;

public enum Major {
    CS("CS"),
    CPIS("CPIS"),
    ENGLISH("English");

    private final String displayName;

    Major(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Major fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (Major major : values()) {
            if (major.name().equalsIgnoreCase(value) || major.displayName.equalsIgnoreCase(value)) {
                return major;
            }
        }
        return null;
    }
}
