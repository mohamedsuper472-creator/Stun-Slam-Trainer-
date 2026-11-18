# Stun Slam Trainer (Fabric 1.21.11) — experimental source

P toggles the visual helper; F4 opens settings; both can be remapped in Controls. Choose axe and mace hotbar slots (1–9). The axe slot is outlined red while the local player is approximately 4.0 blocks above a nearby shield-blocking player. Following a client-observed axe attack, if the opponent stops blocking within 12 ticks, the mace slot becomes red for 30 ticks.

**Important limitation:** Minecraft clients cannot reliably distinguish a real shield disable from voluntary shield lowering or lag without server-side support. This is a best-effort indicator, not confirmed shield-break detection. It does not switch items or automate attacks. Build has not been tested in this environment.

Build with Java 21 and Gradle: `gradle build`. GitHub Actions workflow included. If dependency versions have changed, adjust `gradle.properties` to published Fabric versions for 1.21.11.

V4: Axe outline disappears immediately on an airborne axe attack against the tracked player; cannot reactivate until landing. Mace cue remains a client-side heuristic.
