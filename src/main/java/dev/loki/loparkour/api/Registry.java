package dev.loki.loparkour.api;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.mode.Mode;
import dev.loki.loparkour.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Registers stuff.
 *
 * @author loki
 * @since 5.0.0
 */
public final class Registry {

    private static final LinkedList<Mode> modes = new LinkedList<>();
    private static final LinkedList<Style> styles = new LinkedList<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        modes.add(mode);
    }

    public static void register(@NotNull Style style) {
        styles.add(style);
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return styles.stream()
                .filter(style -> style.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static List<Style> getStyles() {
        return styles;
    }

    public static List<Mode> getModes() {
        return modes;
    }
}
