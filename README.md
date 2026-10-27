# Mystic Kombat

**The Fight Begins.**

Mystic Kombat is a 2D, one-on-one fighting game built with **Java 21**
and **JavaFX**. It brings together a roster of distinct fighters,
character-specific attacks and ultimate abilities, AI opponents,
multiple arenas, and a progression system with Arcade trials, XP,
unlockable fighters, and upgrades.

## Overview

Choose a fighter, enter an arena, trade attacks, build Mystic Energy
(Ki), and unleash an Ultimate Incantation when your meter is ready. Play
against an AI opponent or another player locally, and work through the
Arcade gauntlet to earn XP and improve your fighters.

## Features

-   **One-on-one combat:** real-time fighter movement, attacks, hit
    detection, rounds, and match results.
-   **Fighter roster:** ten character entries with individual
    identities, stats, portraits, and character sprite assets.
-   **Special moves and ultimates:** character-specific abilities
    supported by the combat system.
-   **Mystic Energy (Ki):** build energy during combat and use it to
    activate an ultimate.
-   **AI opponents:** configurable difficulty levels for solo play.
-   **Game modes and screens:** menu, mode selection, character
    selection, arena selection, Arcade, Training, Versus, Combat,
    Results, and Settings screens are represented in the project.
-   **Arcade progression:** a ten-trial gauntlet with trial selection,
    opponents, arenas, and difficulty progression.
-   **XP progression:** the progression manager stores XP, Arcade
    clears, fighter unlocks, and per-fighter stat upgrade levels.
-   **Sorcery Upgradation:** spend XP to unlock fighters and improve
    Health, Damage, and Speed.
-   **Visual and audio assets:** character sprite sheets, portraits,
    effects, background music, and sound effects.
-   **Gamepad support:** controller support is included through Jamepad.

> The exact availability and navigation of individual screens depend on
> the current screen registration and game-state wiring in `Main.java`.
> Check the source if you change the flow.

## Fighter roster

The roster data currently contains these ten fighters:

1.  Mystic Mage
2.  Shadow Vigilante
3.  Speedster Sonic
4.  Plumber Brawler
5.  Flame Karateka
6.  Cyber Hunter
7.  Street Striker
8.  Star Sage
9.  Phantom Blade
10. Toxic Toad

The roster metadata defines each fighter's title, base Health, Speed,
Damage, powers, and display color. Character artwork and portrait assets
are stored under `src/main/resources/assets/`.

## Tech stack

-   Java 21
-   JavaFX 21.0.5 (`javafx-controls`, `javafx-media`)
-   Maven
-   Jamepad 2.26.4.0 for gamepad support

## Requirements

Install the following before running the project:

-   JDK 21
-   Apache Maven
-   A desktop environment supported by JavaFX

Check your installations:

``` bash
java -version
mvn -version
```

## Run locally

1.  Clone the repository:

    ``` bash
    git clone <YOUR_GITHUB_REPOSITORY_URL>
    cd mystic-kombat
    ```

2.  Run the game with Maven:

    ``` bash
    mvn clean javafx:run
    ```

Maven downloads the required dependencies and launches the application
using the configured main class, `mystic.Main`.

## Build

To compile and package the project:

``` bash
mvn clean package
```

The JavaFX Maven plugin is configured in `pom.xml`.

## Project structure

``` text
mystic-kombat/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── mystic/
        │       ├── Main.java
        │       ├── ai/          # AI controller and difficulty logic
        │       ├── combat/      # Combat, attacks, projectiles, ultimates, match logic
        │       ├── common/      # Constants, states, roster, theme, shared types
        │       ├── engine/      # Game loop, screens, input, rendering, progression
        │       ├── fighters/    # Fighter base class and fighter implementations
        │       ├── modes/       # Game mode definitions
        │       └── net/         # Network session code
        └── resources/
            └── assets/
                ├── audio/
                ├── characters/
                ├── data/
                ├── effects/
                ├── portraits/
                └── ui/
```

## Controls

Controls are handled by the input system and can vary by screen and game
mode. The on-screen prompts are the best reference for the current
build.

Typical navigation uses: - **Arrow keys / WASD:** navigate menus or
choose items where prompted. - **Enter / Space:** confirm selections. -
**Escape:** go back or open/close the pause flow where supported. - **Q
/ E / U:** screen-specific actions, including champion selection or
opening Sorcery Upgradation where shown.

For combat, use the in-game control prompts and the action bindings in
`InputManager.java`.

## Progress and save data

`ProgressionManager` saves player progression in the user's home
directory:

``` text
~/.mystic-kombat/save.properties
```

The save data includes XP, Arcade trial clears, unlocked fighter IDs,
and fighter upgrade levels. This file is local to the machine and is not
committed to the repository.

## Assets

Game assets are bundled in:

``` text
src/main/resources/assets/
```

This includes character sprite sheets, portraits, audio files, visual
effects, move data, and a UI font. Keep resource paths consistent with
the paths referenced by the Java source.

## Development notes

-   The Maven project targets Java 21.
-   The application entry point is `mystic.Main`.
-   Game screens are registered in `Main.java` and switched through
    `GameEngine`.
-   Shared roster information is defined in `common/Roster.java`.
-   Combat behavior is primarily implemented in
    `combat/CombatSystem.java` and the fighter classes.
-   Progression and purchases are implemented in
    `engine/ProgressionManager.java`.

## Troubleshooting

**`mvn: command not found`**

Install Maven and reopen your terminal, then check `mvn -version`.

**Java version mismatch**

Make sure `java -version` reports JDK 21. Configure `JAVA_HOME` to point
to your JDK 21 installation if necessary.

**JavaFX launch or native-library errors**

Use a JDK compatible with your operating system and architecture. Then
try:

``` bash
mvn clean
mvn javafx:run
```

**Missing images, sounds, or fonts**

Confirm that the required files are present under
`src/main/resources/assets/` and that resource paths in the code match
their filenames.

## Current project status

This README describes the source files and systems present in the
uploaded project snapshot. It does not claim that every listed screen,
mode, controller, or progression path has been fully tested end-to-end.
Run the project and verify the current menu navigation, character
selection, combat controls, Arcade rewards, and upgrade effects before
describing them as complete in a release.

## Contributing

1.  Create a feature branch.
2.  Make focused changes.
3.  Run `mvn clean package`.
4.  Test the relevant game flow before opening a pull request.

## License

No license file was included in the uploaded project snapshot. Add a
`LICENSE` file before granting reuse or redistribution rights.
