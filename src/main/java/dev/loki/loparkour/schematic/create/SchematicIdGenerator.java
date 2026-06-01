package dev.loki.loparkour.schematic.create;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class SchematicIdGenerator {

    private SchematicIdGenerator() {
    }

    @NotNull
    static String fromSelection(@NotNull Location pos1, @NotNull Location pos2) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (pos1.getWorld() != null) {
                md.update(pos1.getWorld().getUID().toString().getBytes(StandardCharsets.UTF_8));
            }
            md.update((byte) Math.min(pos1.getBlockX(), pos2.getBlockX()));
            md.update((byte) Math.min(pos1.getBlockY(), pos2.getBlockY()));
            md.update((byte) Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
            md.update((byte) Math.max(pos1.getBlockX(), pos2.getBlockX()));
            md.update((byte) Math.max(pos1.getBlockY(), pos2.getBlockY()));
            md.update((byte) Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
            return HexFormat.of().formatHex(md.digest()).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return Long.toHexString(System.nanoTime()).substring(0, 8);
        }
    }
}
