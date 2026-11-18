# Stun Slam Trainer v5 — Fabric 1.21.11

- P toggles the client mod; F4 opens the slot settings (both rebindable in Controls).
- When airborne and around 4 blocks above a shield-holding player, the selected axe slot gets a thin red outline.
- Hitting that target with an axe while airborne removes the axe outline immediately.
- If the target subsequently stops blocking within 12 ticks, the mace slot outline appears until landing.
- All cues reset on landing.

**Limitation:** A client-only mod cannot reliably distinguish a shield being disabled from the opponent simply lowering it. This is a training approximation, not guaranteed real-time shield-break detection.

To build, upload all files to GitHub, then run `.github/workflows/build.yml`. Build not verified in this environment.
