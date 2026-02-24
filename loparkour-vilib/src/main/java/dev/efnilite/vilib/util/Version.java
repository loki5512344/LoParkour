package dev.efnilite.vilib.util;

import org.bukkit.Bukkit;

import java.util.Arrays;

/**
 * Version class, useful for checking whether certain features on legacy can be executed.
 *
 * @author Efnilite
 */
public enum Version {

    V1_16(16), V1_17(17), V1_18(18), V1_19(19), V1_20(20), V1_20_5(20, 5),
    V1_21(21), V1_22(22);

    public static Version VERSION;
    public final int major;
    public final int minor;

    Version(int major) {
        this.major = major;
        this.minor = 0;
    }

    Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Returns whether the version is higher or equal to a given version.
     *
     * @param compareTo The version to compare to
     * @return true if the current version is higher or equal to the given version, false if not
     */
    public static boolean isHigherOrEqual(Version compareTo) {
        if (VERSION.major == compareTo.major) {
            return VERSION.minor >= compareTo.minor;
        } else {
            return VERSION.major > compareTo.major;
        }
    }

    /**
     * Get the current version as a String which can be displayed to users.
     *
     * @return the pretty version as a String
     */
    public static String getPrettyVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /**
     * Returns the current version as an instance of this enum.
     *
     * @return the version.
     */
    public static Version getVersion() {
        var parts = getPrettyVersion().split("\\.");
        var major = Integer.parseInt(parts[1]);
        var minor = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;

        var lowerVersions = Arrays.stream(Version.values()).filter(version -> {
            if (version.major == major) {
                return version.minor <= minor;
            } else {
                return version.major < major;
            }
        }).toList();

        VERSION = lowerVersions.get(lowerVersions.size() - 1);

        return VERSION;
    }

    /**
     * Gets the internal version from the Bukkit package.
     * Format: "v1_20_R1"
     *
     * @return the internal version with format "v1_20_R1"
     */
    @Deprecated(forRemoval = true)
    public static String getInternalVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }
}
