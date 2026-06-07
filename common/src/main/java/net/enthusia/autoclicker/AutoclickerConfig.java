package net.enthusia.autoclicker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

public final class AutoclickerConfig {
    private static final int CONFIG_VERSION = 1;

    private final Path path;
    private ActionMode leftMode = ActionMode.CLICK;
    private ActionMode rightMode = ActionMode.OFF;
    private long leftIntervalMillis = 1_000L;
    private long rightIntervalMillis = 1_000L;
    private long runDurationMillis;

    private AutoclickerConfig(Path path) {
        this.path = path;
    }

    public static AutoclickerConfig load(Path path) {
        AutoclickerConfig config = new AutoclickerConfig(path);
        if (!Files.isRegularFile(path)) {
            config.save();
            return config;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            config.leftMode = parseMode(properties.getProperty("left-mode"), config.leftMode);
            config.rightMode = parseMode(properties.getProperty("right-mode"), config.rightMode);
            config.leftIntervalMillis = parseInterval(
                properties.getProperty("left-interval-ms"),
                config.leftIntervalMillis
            );
            config.rightIntervalMillis = parseInterval(
                properties.getProperty("right-interval-ms"),
                config.rightIntervalMillis
            );
            config.runDurationMillis = parseNonNegative(
                properties.getProperty("run-duration-ms"),
                config.runDurationMillis
            );
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[Enthusia AutoClicker] Could not read config; using safe defaults: "
                + exception.getMessage());
        }
        return config;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("config-version", Integer.toString(CONFIG_VERSION));
        properties.setProperty("left-mode", leftMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("right-mode", rightMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("left-interval-ms", Long.toString(leftIntervalMillis));
        properties.setProperty("right-interval-ms", Long.toString(rightIntervalMillis));
        properties.setProperty("run-duration-ms", Long.toString(runDurationMillis));

        Path parent = path.getParent();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Enthusia AutoClicker client settings");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Enthusia AutoClicker] Could not save config: " + exception.getMessage());
        }
    }

    public ActionMode leftMode() {
        return leftMode;
    }

    public void setLeftMode(ActionMode leftMode) {
        this.leftMode = leftMode;
    }

    public ActionMode rightMode() {
        return rightMode;
    }

    public void setRightMode(ActionMode rightMode) {
        this.rightMode = rightMode;
    }

    public long leftIntervalMillis() {
        return leftIntervalMillis;
    }

    public void setLeftIntervalMillis(long leftIntervalMillis) {
        this.leftIntervalMillis = requireInterval(leftIntervalMillis);
    }

    public long rightIntervalMillis() {
        return rightIntervalMillis;
    }

    public void setRightIntervalMillis(long rightIntervalMillis) {
        this.rightIntervalMillis = requireInterval(rightIntervalMillis);
    }

    public long runDurationMillis() {
        return runDurationMillis;
    }

    public void setRunDurationMillis(long runDurationMillis) {
        if (runDurationMillis < 0L) {
            throw new IllegalArgumentException("Run duration cannot be negative.");
        }
        this.runDurationMillis = runDurationMillis;
    }

    private static ActionMode parseMode(String value, ActionMode fallback) {
        if (value == null) {
            return fallback;
        }
        return ActionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static long parseInterval(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        return requireInterval(Long.parseLong(value));
    }

    private static long parseNonNegative(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        long parsed = Long.parseLong(value);
        if (parsed < 0L) {
            throw new IllegalArgumentException("Duration cannot be negative.");
        }
        return parsed;
    }

    private static long requireInterval(long millis) {
        if (millis < DurationParser.MINIMUM_CLICK_INTERVAL_MILLIS) {
            throw new IllegalArgumentException("Click interval cannot be faster than 20 ticks.");
        }
        return millis;
    }
}
