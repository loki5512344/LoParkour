package dev.loki.loparkour.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color formatting
 * Replaces vilib ColorUtil.color()
 */
public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Converts color codes to Minecraft format
     * Supports both legacy (&) and hex (&#RRGGBB) colors
     *
     * @param text The text to color
     * @return Colored text
     */
    public static String color(String text) {
        if (text == null) {
            return "";
        }

        // Convert hex colors &#RRGGBB to <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);

        // Convert legacy color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
