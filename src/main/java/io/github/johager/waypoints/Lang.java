package io.github.johager.waypoints;

import java.util.Locale;

/** Verfügbare Sprachen / available languages. */
public enum Lang {
    EN(Locale.ENGLISH, "English", "yyyy-MM-dd"),
    DE(Locale.GERMANY, "Deutsch", "dd.MM.yyyy");

    private final Locale locale;
    private final String displayName;
    private final String datePattern;

    Lang(Locale locale, String displayName, String datePattern) {
        this.locale = locale;
        this.displayName = displayName;
        this.datePattern = datePattern;
    }

    public Locale locale() {
        return locale;
    }

    public String displayName() {
        return displayName;
    }

    public String datePattern() {
        return datePattern;
    }

    /** "en", "english", "de", "deutsch", "german" → Sprache, sonst {@code null}. */
    public static Lang parse(String text) {
        if (text == null) {
            return null;
        }
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "en", "english", "englisch" -> EN;
            case "de", "deutsch", "german", "ger" -> DE;
            default -> null;
        };
    }

    /** Wählt die Sprache passend zur Spielsprache, sonst {@code null}. */
    public static Lang fromLocale(Locale locale) {
        if (locale == null) {
            return null;
        }
        return parse(locale.getLanguage());
    }
}
