package mystic.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javafx.scene.media.AudioClip;

/**
 * Central audio manager for Mystic Kombat.
 *
 * Features:
 * - Sound effects for combat actions.
 * - Page turn and book open sound effects for the Tome Character Select.
 * - Looping arena background music.
 * - Independent music and SFX volume controls.
 * - Independent music and SFX mute controls.
 * - Safe handling of missing audio files or unavailable audio devices.
 */
public final class SoundManager {

    public enum Sound {
        LIGHT,
        HEAVY,
        HIT,
        DASH,
        ULTIMATE,
        ROUND,
        PAGE_TURN,
        BOOK_OPEN
    }

    private static final Map<Sound, AudioClip> CLIPS = new EnumMap<>(Sound.class);

    private static AudioClip music;

    private static double volume = 0.65;
    private static double musicVolume = 0.30;

    private static boolean enabled = true;
    private static boolean musicEnabled = true;

    private SoundManager() {
        // Utility class; do not instantiate.
    }

    static {
        initialize();
    }

    /**
     * Loads available sound effects and background music.
     * Missing resources never prevent the game from launching.
     */
    private static void initialize() {
        load(Sound.LIGHT, "/assets/audio/light.wav");
        load(Sound.HEAVY, "/assets/audio/heavy.wav");
        load(Sound.HIT, "/assets/audio/hit.wav");
        load(Sound.DASH, "/assets/audio/dash.wav");
        load(Sound.ULTIMATE, "/assets/audio/ultimate.wav");
        load(Sound.ROUND, "/assets/audio/round.wav");

        // Load or synthesize tome page turn and book open sounds
        loadOrSynthesize(Sound.PAGE_TURN, "/assets/audio/page_turn.wav", true);
        loadOrSynthesize(Sound.BOOK_OPEN, "/assets/audio/book_open.wav", false);

        try {
            URL url = SoundManager.class.getResource(
                    "/assets/audio/arena_theme.wav");

            if (url != null) {
                music = new AudioClip(url.toExternalForm());
                music.setCycleCount(AudioClip.INDEFINITE);
                music.setVolume(musicVolume);
            }
        } catch (RuntimeException | LinkageError ignored) {
            music = null;
        }
    }

    /**
     * Loads a sound effect from src/main/resources.
     */
    private static void load(Sound sound, String resourcePath) {
        try {
            URL url = SoundManager.class.getResource(resourcePath);

            if (url != null) {
                CLIPS.put(sound, new AudioClip(url.toExternalForm()));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Audio is optional.
        }
    }

    private static void loadOrSynthesize(Sound sound, String resourcePath, boolean isPageTurn) {
        try {
            URL url = SoundManager.class.getResource(resourcePath);
            if (url != null) {
                CLIPS.put(sound, new AudioClip(url.toExternalForm()));
                return;
            }
        } catch (Exception ignored) {
        }

        // Fallback procedural generator so no missing wav file can break the game
        try {
            int rate = 44100;
            double duration = isPageTurn ? 0.22 : 0.45;
            int total = (int) (duration * rate);
            byte[] raw = new byte[total * 2];
            java.util.Random r = new java.util.Random();

            for (int i = 0; i < total; i++) {
                double progress = (double) i / total;
                double env = Math.sin(progress * Math.PI) * (1.0 - progress * 0.4);
                double sampleVal;
                if (isPageTurn) {
                    sampleVal = (r.nextDouble() * 2.0 - 1.0) * env * 14000;
                } else {
                    double freq = 90.0 + (1.0 - progress) * 140.0;
                    sampleVal = Math.sin(2.0 * Math.PI * freq * (i / (double) rate)) * env * 17000;
                }
                short val = (short) sampleVal;
                raw[2 * i] = (byte) (val & 0xff);
                raw[2 * i + 1] = (byte) ((val >> 8) & 0xff);
            }

            AudioFormat fmt = new AudioFormat(rate, 16, 1, true, false);
            AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(raw), fmt, total);
            File tempWav = File.createTempFile("mystic_sfx_" + sound.name(), ".wav");
            tempWav.deleteOnExit();
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, tempWav);

            CLIPS.put(sound, new AudioClip(tempWav.toURI().toString()));
        } catch (Exception ignored) {
        }
    }

    /**
     * Plays a sound effect if SFX is enabled.
     */
    public static void play(Sound sound) {
        if (!enabled || sound == null) {
            return;
        }

        AudioClip clip = CLIPS.get(sound);

        if (clip == null) {
            return;
        }

        try {
            clip.setVolume(volume);
            clip.play();
        } catch (RuntimeException | LinkageError ignored) {
            // Never interrupt gameplay because of audio.
        }
    }

    /**
     * Stops a specific sound effect.
     */
    public static void stop(Sound sound) {
        if (sound == null) {
            return;
        }

        AudioClip clip = CLIPS.get(sound);

        if (clip == null) {
            return;
        }

        try {
            clip.stop();
        } catch (RuntimeException | LinkageError ignored) {
            // Ignore unavailable audio backends.
        }
    }

    /**
     * Enables or disables all sound effects.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets sound-effect volume between 0.0 and 1.0.
     */
    public static void setVolume(double value) {
        volume = clamp(value, 0.65);
    }

    public static double getVolume() {
        return volume;
    }

    /**
     * Enables or disables background music.
     */
    public static void setMusicEnabled(boolean value) {
        musicEnabled = value;

        if (music == null) {
            return;
        }

        try {
            if (musicEnabled) {
                music.setVolume(musicVolume);

                if (!music.isPlaying()) {
                    music.play();
                }
            } else {
                music.stop();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Audio is optional.
        }
    }

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    /**
     * Sets music volume between 0.0 and 1.0.
     */
    public static void setMusicVolume(double value) {
        musicVolume = clamp(value, 0.30);

        if (music != null) {
            try {
                music.setVolume(musicVolume);
            } catch (RuntimeException | LinkageError ignored) {
                // Audio is optional.
            }
        }
    }

    public static double getMusicVolume() {
        return musicVolume;
    }

    /**
     * Starts looping background music if music is enabled.
     */
    public static void startMusic() {
        setMusicEnabled(musicEnabled);
    }

    /**
     * Stops background music without changing the user's preference.
     */
    public static void stopMusic() {
        if (music == null) {
            return;
        }

        try {
            music.stop();
        } catch (RuntimeException | LinkageError ignored) {
            // Audio is optional.
        }
    }

    /**
     * Releases audio playback when the application closes.
     */
    public static void shutdown() {
        stopMusic();

        for (AudioClip clip : CLIPS.values()) {
            try {
                clip.stop();
            } catch (RuntimeException | LinkageError ignored) {
                // Audio is optional.
            }
        }
    }

    private static double clamp(double value, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }

        return Math.max(0.0, Math.min(1.0, value));
    }
}