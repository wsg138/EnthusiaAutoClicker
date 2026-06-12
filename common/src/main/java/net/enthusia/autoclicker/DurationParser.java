package net.enthusia.autoclicker;

import java.math.BigDecimal;
import java.util.Locale;

public final class DurationParser {
    public static final long MINIMUM_CLICK_INTERVAL_MILLIS = 625L;

    private DurationParser() {
    }

    public static long parseIntervalMillis(String value) {
        long millis = parseMillis(value);
        if (millis < MINIMUM_CLICK_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Click intervals must be at least 12.5 ticks (625ms).");
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

    public static long parseIntervalTicks(String value) {
        long millis = parseTicks(value, false);
        if (millis < MINIMUM_CLICK_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Click intervals must be at least 12.5 ticks.");
        }
        return millis;
    }

    public static long parseOptionalDurationTicks(String value) {
        return parseTicks(value, true);
    }

    public static String formatTicks(long millis) {
        if (millis == 0L) {
            return "0";
        }
        return BigDecimal.valueOf(millis)
            .divide(BigDecimal.valueOf(50L))
            .stripTrailingZeros()
            .toPlainString();
    }

    public static long parseMillis(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Enter a duration such as 12.5t, 625ms, or 2s.");
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
            BigDecimal amount = new BigDecimal(number.trim());
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Duration must be greater than zero.");
            }
            return amount.multiply(BigDecimal.valueOf(multiplier)).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(
                "Duration is too large or does not resolve to whole milliseconds.",
                exception
            );
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

    private static long parseTicks(String value, boolean allowZero) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Enter a number of ticks.");
        }
        try {
            BigDecimal ticks = new BigDecimal(normalized);
            if (ticks.signum() == 0 && allowZero) {
                return 0L;
            }
            if (ticks.signum() <= 0) {
                throw new IllegalArgumentException("Ticks must be greater than zero.");
            }
            return ticks.multiply(BigDecimal.valueOf(50L)).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(
                "Ticks must resolve to whole milliseconds.",
                exception
            );
        }
    }
}
