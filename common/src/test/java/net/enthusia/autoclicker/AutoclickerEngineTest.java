package net.enthusia.autoclicker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutoclickerEngineTest {
    @TempDir
    Path tempDirectory;

    @Test
    void pausesAllInputWhenUnsafe() {
        AutoclickerConfig config = config();
        AutoclickerEngine engine = new AutoclickerEngine();
        engine.setEnabled(true, config, 0L);

        AutoclickerEngine.TickDecision decision = engine.decide(config, 0L, false, false);

        assertFalse(decision.holdLeft());
        assertFalse(decision.clickLeft());
        assertFalse(decision.holdRight());
        assertFalse(decision.clickRight());
    }

    @Test
    void suppressesAttackWhileUsingAnItem() {
        AutoclickerConfig config = config();
        config.setLeftMode(ActionMode.HOLD);
        config.setRightMode(ActionMode.HOLD);
        AutoclickerEngine engine = new AutoclickerEngine();
        engine.setEnabled(true, config, 0L);

        AutoclickerEngine.TickDecision decision = engine.decide(config, 50L, true, true);

        assertFalse(decision.holdLeft());
        assertFalse(decision.clickLeft());
        assertTrue(decision.holdRight());
    }

    @Test
    void allowsPeriodicLeftClicksAfterRightHoldHasStarted() {
        AutoclickerConfig config = config();
        config.setLeftMode(ActionMode.CLICK);
        config.setRightMode(ActionMode.HOLD);
        AutoclickerEngine engine = new AutoclickerEngine();
        engine.setEnabled(true, config, 0L);

        assertFalse(engine.decide(config, 0L, true, false).clickLeft());
        assertTrue(engine.decide(config, 50L, true, false).clickLeft());
        assertFalse(engine.decide(config, 1_049L, true, false).clickLeft());
        assertTrue(engine.decide(config, 1_050L, true, false).clickLeft());
    }

    @Test
    void enforcesConfiguredClickInterval() {
        AutoclickerConfig config = config();
        AutoclickerEngine engine = new AutoclickerEngine();
        engine.setEnabled(true, config, 0L);

        assertTrue(engine.decide(config, 0L, true, false).clickLeft());
        assertFalse(engine.decide(config, 999L, true, false).clickLeft());
        assertTrue(engine.decide(config, 1_000L, true, false).clickLeft());
    }

    @Test
    void timedRunStopsAtDeadline() {
        AutoclickerConfig config = config();
        config.setRunDurationMillis(2_000L);
        AutoclickerEngine engine = new AutoclickerEngine();
        engine.setEnabled(true, config, 100L);

        assertFalse(engine.updateTimer(2_099L));
        assertTrue(engine.updateTimer(2_100L));
        assertFalse(engine.isEnabled());
    }

    private AutoclickerConfig config() {
        return AutoclickerConfig.load(tempDirectory.resolve("config.properties"));
    }
}
