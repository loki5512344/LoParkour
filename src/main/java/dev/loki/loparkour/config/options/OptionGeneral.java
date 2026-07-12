package dev.loki.loparkour.config.options;

import dev.loki.loparkour.LoParkour;
import dev.loki.loparkour.config.core.Config;
import dev.loki.loparkour.menu.core.ParkourOption;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General configuration options (heading, border, leads, go-back location, parkour options).
 */
public class OptionGeneral {

    private OptionGeneral() {
    }

    public static double BORDER_SIZE;
    public static List<Integer> POSSIBLE_LEADS;
    public static BlockFace HEADING;
    public static Location GO_BACK_LOC;

    public static Map<ParkourOption, Boolean> OPTIONS_ENABLED;
    public static Map<ParkourOption, String>  OPTIONS_DEFAULTS;

    public static void init(boolean firstLoad) {
        initGoBackLocation();
        initLeads();
        initHeading();

        if (firstLoad) {
            BORDER_SIZE = Config.GENERATION.getDouble("advanced.border-size");
        }
    }

    public static void initOptions() {
        List<ParkourOption> options = new ArrayList<>(Arrays.asList(ParkourOption.values()));
        options.remove(ParkourOption.JOIN);
        options.remove(ParkourOption.ADMIN);

        OPTIONS_DEFAULTS = new HashMap<>();
        OPTIONS_ENABLED  = new HashMap<>();

        for (ParkourOption option : options) {
            String parent = "default-values." + option.path;
            OPTIONS_ENABLED.put(option, Config.CONFIG.getBoolean(parent + ".enabled"));

            String defaultPath = parent + ".default";
            if (Config.CONFIG.isPath(defaultPath)) {
                String value = Config.CONFIG.getString(defaultPath);

                // Sanitize legacy Boolean values for LANG option
                if (option == ParkourOption.LANG &&
                    ("true".equals(value) || "false".equals(value) || "1".equals(value) || "0".equals(value))) {
                    value = "en";
                    LoParkour.getPlugin().getLogger().warning(
                        "LANG option had invalid Boolean value, reset to 'en'");
                }

                // Debug logging
                if (option == ParkourOption.LANG) {
                    LoParkour.getPlugin().getLogger().info(
                        "LANG option: path=" + defaultPath + ", value=" + value);
                }

                if (value != null && !value.isEmpty()) {
                    OPTIONS_DEFAULTS.put(option, value);
                }
            } else {
                // Debug: path not found
                if (option == ParkourOption.LANG) {
                    LoParkour.getPlugin().getLogger().warning("LANG default path not found: " + defaultPath);
                    LoParkour.getPlugin().getLogger().warning(
                        "Available keys: " + Config.CONFIG.fileConfiguration.getKeys(true));
                }
            }
        }
    }

    private static void initGoBackLocation() {
        GO_BACK_LOC = parseLocation(Config.CONFIG.getString("bungeecord.go-back"));
        try {
            String axesRaw = Config.CONFIG.getString("bungeecord.go-back-axes");
            if (!axesRaw.isEmpty()) {
                String[] axes = axesRaw.split(",");
                if (axes.length >= 2) {
                    GO_BACK_LOC.setPitch(Float.parseFloat(axes[0].trim()));
                    GO_BACK_LOC.setYaw(Float.parseFloat(axes[1].trim()));
                }
            }
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning("Invalid bungeecord.go-back-axes, using 0,0");
        }
    }

    private static void initLeads() {
        POSSIBLE_LEADS = Config.CONFIG.getIntList("options.leads.amount");
        for (int lead : new ArrayList<>(POSSIBLE_LEADS)) {
            if (lead < 1 || lead > 128) {
                LoParkour.getPlugin().getLogger().severe(
                    "Invalid lead: %d. Must be 1–128.".formatted(lead));
                POSSIBLE_LEADS.remove((Object) lead);
            }
        }
    }

    private static void initHeading() {
        String heading = Config.GENERATION.getString("advanced.island.parkour.heading");
        HEADING = switch (heading.toLowerCase()) {
            case "north" -> BlockFace.NORTH;
            case "south" -> BlockFace.SOUTH;
            case "west"  -> BlockFace.WEST;
            case "east"  -> BlockFace.EAST;
            default -> {
                LoParkour.getPlugin().getLogger().severe("Invalid heading: " + heading);
                yield BlockFace.NORTH;
            }
        };
    }

    private static Location parseLocation(String location) {
        try {
            String[] values = location.replaceAll("[()]", "").replaceAll("[, ]", " ").trim().split("\\s+");
            if (values.length < 4) {
                throw new IllegalArgumentException("Expected 4 values (x,y,z,world), got " + values.length);
            }
            World world = Bukkit.getWorld(values[3]);
            if (world == null) {
                world = Bukkit.getWorlds().get(0);
            }
            return new Location(world, Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]), Double.parseDouble(values[2]));
        } catch (Exception ex) {
            LoParkour.getPlugin().getLogger().warning(
                    "Invalid bungeecord.go-back value '" + location + "'. Falling back to spawn world origin.");
            World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            return new Location(fallback, 0, 100, 0);
        }
    }
}
