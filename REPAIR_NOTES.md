# Mystic Kombat repair notes

## Changes in this pass
- Added a small sound-effect manager with safe fallbacks if JavaFX audio is unavailable.
- Added original generated WAV effects for light attacks, heavy attacks, dashes/dodges, confirmed damage, round starts, and Ultimate Incantation.
- Added a generated ambient arena theme that loops while enabled.
- Connected the Settings screen's sound/music volume and mute toggles to the audio manager.
- Corrected the keyboard-controls help screen to match the actual `InputManager` bindings.
- Repaired the two additional fighter classes so they use the current shared `Fighter` constructor and ultimate API rather than an obsolete constructor/render/attack API.

## Current default controls
### Player 1
- Move: A / D
- Jump: W
- Crouch: S
- Light: J
- Heavy: K
- Block: F
- Dash: L
- Dodge: I
- Launcher: H
- Ultimate: U

### Player 2
- Move: Left / Right
- Jump: Up
- Crouch: Down
- Light: Numpad 1
- Heavy: Numpad 2
- Block: Period
- Dash: Numpad 3
- Dodge: Numpad 4
- Launcher: Numpad 5
- Ultimate: Numpad 0

## Build verification
The project is configured for Java 21 and JavaFX 21.0.5. Automated Maven build verification could not be run in the editing environment because Maven (`mvn`) is not installed. Please verify locally with:

```bash
mvn clean package
mvn javafx:run
```

A successful compile is not the same as full gameplay QA. Test both players' controls, blocking, hit detection, rounds, ultimate effects, pause/resume, and audio on your Mac after building.
