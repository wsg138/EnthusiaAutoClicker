# Combat hook discovery performance - SPEAR

## Spec
- When CombatX is unavailable, the server shall perform at most one automatic discovery per 30 seconds, independent of player count and tick frequency.
- When the explicit settings reload runs, the hook shall retry discovery immediately.
- When any plugin enables or disables, the hook shall invalidate its cached API and rediscover on the next check, avoiding a stale provider reference.
- While the hook is unavailable, require-combatx shall remain fail-closed and the optional internal PvP fallback shall remain unchanged.
- A valid provider shall remain cached; failed combat invocations shall continue to block the current tick.

## Scope and provenance
Base: a29dac9 (current canonical wsg138/EnthusiaAutoClicker main on 2026-10-26).
Server-only change; no client protocol, permissions, config keys, data or click targeting changes.
Existing checkout has a modified gradlew.bat and is preserved; changes use an isolated worktree.
No project EARS/state helpers or pre-existing SPEAR documents were found. This record supplies bounded requirements/tasks/evidence instead.

## Prove / engine / arch / refine
Red: the original discovery behavior (with injected clock/discovery seam) failed 2 of 3 regressions: 10,000 availability checks caused 10,000 discoveries, and automatic rediscovery happened before the 30-second boundary.

Engine: cache failed discovery using System.nanoTime elapsed subtraction (including wraparound); explicit refresh invalidates immediately; synchronous Bukkit plugin enable/disable callbacks invalidate the cached method/result for lazy rediscovery. Discovery and invocation stay on the existing main-thread path; no async Bukkit work or new scheduler.

Architecture: only CombatXHook and its event registration change. AutoClickService.stopForCombatState still rejects unavailable required CombatX and always checks the internal tracker; reflective invocation failure still returns combat=true. No configuration/permission/client protocol changes, new dependencies or companion API migration. The standalone Maven server-plugin workflow owns this artifact; no submodule pin is present in this repository.

Green: canonical server-plugin Maven clean verify and PMD 3.26.0 pass on local JDK22, release17 / Paper API1.20.4. 46 tests, no failures/errors/skips, including 7 hook regressions (miss cache, delayed recovery, explicit refresh, lifecycle callback invalidation, positive cache, monotonic wraparound, combat-check miss cache). PMD initially ran under JDK25 and emitted unsupported-class parser errors despite a success status; that result is rejected, and the complete command was rerun successfully under JDK22 without parser errors. PMD method-reference false positive was resolved using a directly visible lambda call, without suppressing rules.

Packaging: public client evidence API classes retained; no JUnit/Bukkit classes bundled. Source review inspected the combat safety gates above. Hosted CI and real Paper26.2 / CombatX / client acceptance remain separate evidence. No live performance improvement is claimed.
Tests inject a monotonic clock and discovery operation into the actual hook; they do not claim real CombatX/Paper/client acceptance.
No shared TEST/client usage, production writes, deployment or restart is authorized by this work.
