package net.enthusia.autoclicker;

import java.util.Locale;

public final class DurationParser {
    public static final long MINIMUM_CLICK_INTERVAL_MILLIS = 1_000L;

    private DurationParser() {
    }

    public static long parseIntervalMillis(String value) {
        long millis = parseMillis(value);
        if (millis < MINIMUM_CLICK_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Click intervals must be at least 20 ticks (1000ms).");
        }
        return millis;
    }

    public static long parseOptionalDurationMillis(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("off") || normalized.equals("unlimited") || normalized.equals("0")) {
            return 0L;
        }
        return parseMillis(normalized);
    }

    public static long parseMillis(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Enter a duration such as 20t, 1000ms, or 2s.");
        }

        long multiplier;
        String number;
        if (normalized.endsWith("ms")) {
            multiplier = 1L;
            number = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("t")) {
            multiplier = 50L;
            number = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("s")) {
            multiplier = 1_000L;
            number = normalized.substring(0, normalized.length() - 1);
        } else {
            throw new IllegalArgumentException("Use a t, ms, or s suffix.");
        }

        try {
            long amount = Long.parseLong(number.trim());
            if (amount <= 0L) {
                throw new IllegalArgumentException("Duration must be greater than zero.");
            }
            return Math.multiplyExact(amount, multiplier);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too large or is not a whole number.", exception);
        }
    }

    public static String format(long millis) {
        if (millis == 0L) {
            return "off";
        }
        if (millis % 50L == 0L) {
            return (millis / 50L) + "t";
        }
        return millis + "ms";
    }
}
