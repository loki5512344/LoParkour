package dev.loki.loparkour;

import dev.loki.loparkour.bootstrap.PluginBootstrap;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.hook.papi.PAPIHook;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.reward.core.Rewards;
import dev.loki.loparkour.schematic.core.SchematicManager;
import dev.loki.loparkour.storage.Storage;
import dev.loki.loparkour.world.core.Divider;
import dev.loki.loparkour.world.core.World;
import dev.lolib.core.LoPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;

/**
 * Main class of LoParkour
 *
 * @author loki
 */
public final class LoParkour extends LoPlugin {

    public static final String NAME = "&#FF6464&lLoParkour&r";
    public static final String PREFIX = NAME + " &#404040» &#A0A0A0";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static LoParkour instance;
    private static SchematicManager schematicManager;

    @Nullable
    private static PAPIHook placeholderHook;

    public static Gson getGson() {
        return gson;
    }

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            LoParkour.getPlugin().getLogger().info("[Debug] " + message);
        }
    }

    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }

    public static LoParkour getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    public static SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void setSchematicManager(@NotNull SchematicManager manager) {
        schematicManager = manager;
    }

    public void setPlaceholderHook(@Nullable PAPIHook hook) {
        placeholderHook = hook;
    }

    @Override
    public void enable() {
        instance = this;
        PluginBootstrap.enable(this);
    }

    @Override
    public void disable() {
        PluginBootstrap.disable(this);
    }

    /** Called from {@link PluginBootstrap} on shutdown. */
    public void runShutdownSequence() {
        try {
            new ArrayList<>(Divider.sections.keySet()).forEach(session -> {
                try {
                    session.onAllPlayersLeft();
                } catch (Exception e) {
                    getLogger().warning("Error cleaning up session: " + e.getMessage());
                }
            });

            new ArrayList<>(Divider.sections.keySet()).forEach(Divider::remove);

            for (ParkourUser user : ParkourUser.getUsers()) {
                ParkourUser.leave(user);
            }

            if (Modes.DEFAULT != null && Modes.DEFAULT.getLeaderboard() != null) {
                Modes.DEFAULT.getLeaderboard().write(false);
            }

            Rewards.clear();
            dev.loki.loparkour.adaptive.bootstrap.AdaptiveServices.shutdown();
            Storage.close();
            World.delete();
        } catch (Throwable t) {
            getLogger().severe("Error during plugin shutdown: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
