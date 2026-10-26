package net.enthusia.autoclicker.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatXHookTest {
    @Test
    void unavailableApiIsNotRediscoveredForEveryPlayerAndTick() {
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(() -> 0L, () -> {
            discoveries.incrementAndGet();
            return false;
        });
        for (int check = 0; check < 10_000; check++) {
            assertFalse(hook.isAvailable());
        }
        assertEquals(1, discoveries.get());
    }

    @Test
    void unavailableApiCanRecoverAfterThirtySeconds() {
        AtomicLong clock = new AtomicLong(42L);
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(clock::get, () -> discoveries.incrementAndGet() > 1);
        assertFalse(hook.isAvailable());
        clock.addAndGet(29_999_999_999L);
        assertFalse(hook.isAvailable());
        clock.incrementAndGet();
        assertTrue(hook.isAvailable());
        assertTrue(hook.isAvailable());
        assertEquals(2, discoveries.get());
    }

    @Test
    void explicitRefreshRetriesImmediately() {
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(() -> 0L, () -> discoveries.incrementAndGet() > 1);
        assertFalse(hook.isAvailable());
        assertTrue(hook.refresh());
        assertEquals(2, discoveries.get());
    }

    @Test
    void pluginLifecycleChangesAllowImmediateRecovery() {
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(() -> 0L, () -> discoveries.incrementAndGet() > 1);
        assertFalse(hook.isAvailable());
        hook.onPluginEnable(null);
        assertTrue(hook.isAvailable());
        hook.onPluginDisable(null);
        assertTrue(hook.isAvailable());
        assertEquals(3, discoveries.get());
    }

    @Test
    void successfulDiscoveryStaysCachedBeyondRetryWindow() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(clock::get, () -> {
            discoveries.incrementAndGet();
            return true;
        });
        assertTrue(hook.isAvailable());
        clock.set(90_000_000_000L);
        assertTrue(hook.isAvailable());
        assertEquals(1, discoveries.get());
    }

    @Test
    void retryIntervalWorksAcrossMonotonicClockWraparound() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 1L);
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(clock::get, () -> discoveries.incrementAndGet() > 1);
        assertFalse(hook.isAvailable());
        clock.addAndGet(29_999_999_999L);
        assertFalse(hook.isAvailable());
        clock.incrementAndGet();
        assertTrue(hook.isAvailable());
    }

    @Test
    void combatChecksAlsoReuseAnUnavailableResult() {
        AtomicInteger discoveries = new AtomicInteger();
        CombatXHook hook = new CombatXHook(() -> 0L, () -> {
            discoveries.incrementAndGet();
            return false;
        });
        for (int check = 0; check < 10_000; check++) {
            assertFalse(hook.isInCombat(null));
        }
        assertEquals(1, discoveries.get());
    }
}
