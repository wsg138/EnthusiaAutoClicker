package net.enthusia.autoclicker;

public final class AutoclickerEngine {
    private boolean enabled;
    private long stopAtMillis;
    private long nextLeftClickMillis;
    private long nextRightClickMillis;
    private boolean rightHoldActive;

    public void toggle(AutoclickerConfig config, long nowMillis) {
        setEnabled(!enabled, config, nowMillis);
    }

    public void setEnabled(boolean enabled, AutoclickerConfig config, long nowMillis) {
        this.enabled = enabled;
        if (!enabled) {
            stopAtMillis = 0L;
            rightHoldActive = false;
            return;
        }

        stopAtMillis = config.runDurationMillis() == 0L
            ? 0L
            : nowMillis + config.runDurationMillis();
        nextLeftClickMillis = nowMillis;
        nextRightClickMillis = nowMillis;
    }

    public boolean updateTimer(long nowMillis) {
        if (enabled && stopAtMillis > 0L && nowMillis >= stopAtMillis) {
            enabled = false;
            stopAtMillis = 0L;
            return true;
        }
        return false;
    }

    public TickDecision decide(
        AutoclickerConfig config,
        long nowMillis,
        boolean safe,
        boolean usingItem,
        boolean foodActive
    ) {
        if (!enabled || !safe) {
            rightHoldActive = false;
            return TickDecision.NONE;
        }
        if (foodActive) {
            rightHoldActive = false;
            return TickDecision.FOOD;
        }

        boolean holdRight = config.rightEnabled() && config.rightMode() == ActionMode.HOLD;
        boolean clickRight = config.rightEnabled()
            && isDue(config.rightMode(), nowMillis, nextRightClickMillis);
        if (clickRight) {
            nextRightClickMillis = nowMillis + config.rightIntervalMillis();
        }

        boolean rightStartsThisTick = (holdRight && !rightHoldActive) || clickRight;
        rightHoldActive = holdRight;
        boolean holdLeft = config.leftEnabled()
            && config.leftMode() == ActionMode.HOLD
            && !usingItem
            && !rightStartsThisTick;
        boolean clickLeft = config.leftEnabled()
            && !usingItem
            && !rightStartsThisTick
            && isDue(config.leftMode(), nowMillis, nextLeftClickMillis);
        if (clickLeft) {
            nextLeftClickMillis = nowMillis + config.leftIntervalMillis();
        }

        return new TickDecision(holdLeft, clickLeft, holdRight, clickRight, false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static boolean isDue(ActionMode mode, long nowMillis, long dueMillis) {
        return mode == ActionMode.CLICK && nowMillis >= dueMillis;
    }

    public record TickDecision(
        boolean holdLeft,
        boolean clickLeft,
        boolean holdRight,
        boolean clickRight,
        boolean holdFood
    ) {
        public static final TickDecision NONE = new TickDecision(false, false, false, false, false);
        public static final TickDecision FOOD = new TickDecision(false, false, false, false, true);
    }
}
