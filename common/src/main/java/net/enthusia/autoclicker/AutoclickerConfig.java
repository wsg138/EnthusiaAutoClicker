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
    private static final int CONFIG_VERSION = 3;

    public static final boolean DEFAULT_LEFT_ENABLED = true;
    public static final boolean DEFAULT_RIGHT_ENABLED = false;
    public static final boolean DEFAULT_FOOD_ENABLED = false;
    public static final boolean DEFAULT_STATUS_MESSAGES = true;
    public static final ActionMode DEFAULT_LEFT_MODE = ActionMode.CLICK;
    public static final ActionMode DEFAULT_RIGHT_MODE = ActionMode.CLICK;
    public static final long DEFAULT_LEFT_INTERVAL_MILLIS = 1_000L;
    public static final long DEFAULT_RIGHT_INTERVAL_MILLIS = 1_000L;
    public static final long DEFAULT_RUN_DURATION_MILLIS = 0L;
    public static final int DEFAULT_FOOD_LEVEL_THRESHOLD = 18;
    public static final boolean DEFAULT_DURABILITY_GUARD = false;
    public static final int DEFAULT_MINIMUM_DURABILITY = 10;
    public static final boolean DEFAULT_ARMOR_STAND_EATING = false;
    public static final boolean DEFAULT_AUTO_RESTOCK = false;
    public static final int DEFAULT_RESTOCK_AT_COUNT = 4;
    public static final boolean DEFAULT_STOP_WHEN_OUT_OF_FOOD = false;

    private final Path path;
    private boolean leftEnabled = DEFAULT_LEFT_ENABLED;
    private boolean rightEnabled = DEFAULT_RIGHT_ENABLED;
    private boolean foodEnabled = DEFAULT_FOOD_ENABLED;
    private boolean statusMessages = DEFAULT_STATUS_MESSAGES;
    private ActionMode leftMode = DEFAULT_LEFT_MODE;
    private ActionMode rightMode = DEFAULT_RIGHT_MODE;
    private long leftIntervalMillis = DEFAULT_LEFT_INTERVAL_MILLIS;
    private long rightIntervalMillis = DEFAULT_RIGHT_INTERVAL_MILLIS;
    private long runDurationMillis = DEFAULT_RUN_DURATION_MILLIS;
    private int foodLevelThreshold = DEFAULT_FOOD_LEVEL_THRESHOLD;
    private boolean durabilityGuard = DEFAULT_DURABILITY_GUARD;
    private int minimumDurability = DEFAULT_MINIMUM_DURABILITY;
    private boolean armorStandEating = DEFAULT_ARMOR_STAND_EATING;
    private boolean autoRestock = DEFAULT_AUTO_RESTOCK;
    private int restockAtCount = DEFAULT_RESTOCK_AT_COUNT;
    private boolean stopWhenOutOfFood = DEFAULT_STOP_WHEN_OUT_OF_FOOD;

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
            String leftModeValue = properties.getProperty("left-mode");
            String rightModeValue = properties.getProperty("right-mode");
            config.leftEnabled = parseBoolean(
                properties.getProperty("left-enabled"),
                legacyEnabled(leftModeValue, config.leftEnabled)
            );
            config.rightEnabled = parseBoolean(
                properties.getProperty("right-enabled"),
                legacyEnabled(rightModeValue, config.rightEnabled)
            );
            config.foodEnabled = parseBoolean(properties.getProperty("food-enabled"), config.foodEnabled);
            config.statusMessages = parseBoolean(
                properties.getProperty("status-messages"),
                config.statusMessages
            );
            config.leftMode = parseMode(leftModeValue, config.leftMode);
            config.rightMode = parseMode(rightModeValue, config.rightMode);
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
            config.foodLevelThreshold = parseFoodLevel(
                properties.getProperty("food-level-threshold"),
                config.foodLevelThreshold
            );
            config.durabilityGuard = parseBoolean(
                properties.getProperty("durability-guard"),
                config.durabilityGuard
            );
            config.minimumDurability = parseMinimumDurability(
                properties.getProperty("minimum-durability"),
                config.minimumDurability
            );
            config.armorStandEating = parseBoolean(
                properties.getProperty("armor-stand-eating"),
                config.armorStandEating
            );
            config.autoRestock = parseBoolean(
                properties.getProperty("auto-restock"),
                config.autoRestock
            );
            config.restockAtCount = parseRestockCount(
                properties.getProperty("restock-at-count"),
                config.restockAtCount
            );
            config.stopWhenOutOfFood = parseBoolean(
                properties.getProperty("stop-when-out-of-food"),
                config.stopWhenOutOfFood
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
        properties.setProperty("left-enabled", Boolean.toString(leftEnabled));
        properties.setProperty("right-enabled", Boolean.toString(rightEnabled));
        properties.setProperty("food-enabled", Boolean.toString(foodEnabled));
        properties.setProperty("status-messages", Boolean.toString(statusMessages));
        properties.setProperty("left-mode", leftMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("right-mode", rightMode.name().toLowerCase(Locale.ROOT));
        properties.setProperty("left-interval-ms", Long.toString(leftIntervalMillis));
        properties.setProperty("right-interval-ms", Long.toString(rightIntervalMillis));
        properties.setProperty("run-duration-ms", Long.toString(runDurationMillis));
        properties.setProperty("food-level-threshold", Integer.toString(foodLevelThreshold));
        properties.setProperty("durability-guard", Boolean.toString(durabilityGuard));
        properties.setProperty("minimum-durability", Integer.toString(minimumDurability));
        properties.setProperty("armor-stand-eating", Boolean.toString(armorStandEating));
        properties.setProperty("auto-restock", Boolean.toString(autoRestock));
        properties.setProperty("restock-at-count", Integer.toString(restockAtCount));
        properties.setProperty("stop-when-out-of-food", Boolean.toString(stopWhenOutOfFood));

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
        this.leftMode = java.util.Objects.requireNonNull(leftMode);
    }

    public boolean leftEnabled() {
        return leftEnabled;
    }

    public void setLeftEnabled(boolean leftEnabled) {
        this.leftEnabled = leftEnabled;
    }

    public ActionMode rightMode() {
        return rightMode;
    }

    public void setRightMode(ActionMode rightMode) {
        this.rightMode = java.util.Objects.requireNonNull(rightMode);
    }

    public boolean rightEnabled() {
        return rightEnabled;
    }

    public void setRightEnabled(boolean rightEnabled) {
        this.rightEnabled = rightEnabled;
    }

    public boolean foodEnabled() {
        return foodEnabled;
    }

    public void setFoodEnabled(boolean foodEnabled) {
        this.foodEnabled = foodEnabled;
    }

    public boolean statusMessages() {
        return statusMessages;
    }

    public void setStatusMessages(boolean statusMessages) {
        this.statusMessages = statusMessages;
    }

    public int foodLevelThreshold() {
        return foodLevelThreshold;
    }

    public void setFoodLevelThreshold(int foodLevelThreshold) {
        this.foodLevelThreshold = requireFoodLevel(foodLevelThreshold);
    }

    public boolean durabilityGuard() {
        return durabilityGuard;
    }

    public void setDurabilityGuard(boolean durabilityGuard) {
        this.durabilityGuard = durabilityGuard;
    }

    public int minimumDurability() {
        return minimumDurability;
    }

    public void setMinimumDurability(int minimumDurability) {
        this.minimumDurability = requireMinimumDurability(minimumDurability);
    }

    public boolean armorStandEating() {
        return armorStandEating;
    }

    public void setArmorStandEating(boolean armorStandEating) {
        this.armorStandEating = armorStandEating;
    }

    public boolean autoRestock() {
        return autoRestock;
    }

    public void setAutoRestock(boolean autoRestock) {
        this.autoRestock = autoRestock;
    }

    public int restockAtCount() {
        return restockAtCount;
    }

    public void setRestockAtCount(int restockAtCount) {
        this.restockAtCount = requireRestockCount(restockAtCount);
    }

    public boolean stopWhenOutOfFood() {
        return stopWhenOutOfFood;
    }

    public void setStopWhenOutOfFood(boolean stopWhenOutOfFood) {
        this.stopWhenOutOfFood = stopWhenOutOfFood;
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
        if (value.trim().equalsIgnoreCase("off")) {
            return fallback;
        }
        return ActionMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean legacyEnabled(String modeValue, boolean fallback) {
        return modeValue == null ? fallback : !modeValue.trim().equalsIgnoreCase("off");
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
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
            throw new IllegalArgumentException("Click interval cannot be faster than 12.5 ticks.");
        }
        return millis;
    }

    private static int parseFoodLevel(String value, int fallback) {
        return value == null ? fallback : requireFoodLevel(Integer.parseInt(value));
    }

    private static int requireFoodLevel(int foodLevel) {
        if (foodLevel < 1 || foodLevel > 19) {
            throw new IllegalArgumentException("Food level threshold must be between 1 and 19.");
        }
        return foodLevel;
    }

    private static int parseMinimumDurability(String value, int fallback) {
        return value == null ? fallback : requireMinimumDurability(Integer.parseInt(value));
    }

    private static int requireMinimumDurability(int durability) {
        if (durability < 1 || durability > 10_000) {
            throw new IllegalArgumentException("Minimum durability must be between 1 and 10000.");
        }
        return durability;
    }

    private static int parseRestockCount(String value, int fallback) {
        return value == null ? fallback : requireRestockCount(Integer.parseInt(value));
    }

    private static int requireRestockCount(int count) {
        if (count < 0 || count > 63) {
            throw new IllegalArgumentException("Restock count must be between 0 and 63.");
        }
        return count;
    }
}
