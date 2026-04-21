package util;

import model.Major;
import model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CsvUtils {
    private CsvUtils() {
    }

    private static final Pattern SPLIT_COMMA_OUTSIDE_QUOTES =
            Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    public static String toCsvLine(Person person) {
        return String.join(",",
                escape(person.getFirstName()),
                escape(person.getLastName()),
                escape(person.getDepartment()),
                escape(person.getMajor() == null ? "" : person.getMajor().name()),
                escape(person.getEmail()),
                escape(person.getImageURL()));
    }

    public static Person fromCsvLine(String line) {
        List<String> tokens = parseLine(line);
        if (tokens.size() != 6) {
            return null;
        }

        Major major = Major.fromValue(tokens.get(3));
        if (major == null) {
            return null;
        }

        return new Person(tokens.get(0), tokens.get(1), tokens.get(2), major, tokens.get(4), tokens.get(5));
    }

    public static List<String> parseLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        String[] split = SPLIT_COMMA_OUTSIDE_QUOTES.split(line, -1);
        for (String token : split) {
            String normalized = token.trim();
            if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
                normalized = normalized.substring(1, normalized.length() - 1).replace("\"\"", "\"");
            }
            tokens.add(normalized);
        }
        return tokens;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
