package dev.loki.loparkour.api.core;

import dev.loki.loparkour.mode.base.Mode;
import dev.loki.loparkour.style.core.Style;
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

    private Registry() {
    }

    private static final LinkedList<Mode> MODES = new LinkedList<>();
    private static final LinkedList<Style> STYLES = new LinkedList<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        MODES.add(mode);
    }

    public static void register(@NotNull Style style) {
        STYLES.add(style);
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return MODES.stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return STYLES.stream()
                .filter(style -> style.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static List<Style> getStyles() {
        return STYLES;
    }

    public static List<Mode> getModes() {
        return MODES;
    }
}
