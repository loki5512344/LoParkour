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
    private static final Pattern MINIMESSAGE_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    /**
     * Converts color codes to Minecraft format
     * Supports legacy (&), hex (&#RRGGBB), and MiniMessage (<#RRGGBB>) colors
     *
     * @param text The text to color
     * @return Colored text
     */
    public static String color(String text) {
        if (text == null) {
            return "";
        }

        // Convert MiniMessage hex colors <#RRGGBB> to ChatColor
        Matcher miniMatcher = MINIMESSAGE_HEX_PATTERN.matcher(text);
        StringBuffer miniBuffer = new StringBuffer();
        while (miniMatcher.find()) {
            String hexCode = miniMatcher.group(1);
            miniMatcher.appendReplacement(miniBuffer, ChatColor.of("#" + hexCode).toString());
        }
        miniMatcher.appendTail(miniBuffer);
        text = miniBuffer.toString();

        // Convert MiniMessage formatting tags to legacy codes
        text = text.replace("<bold>", "&l")
                   .replace("<italic>", "&o")
                   .replace("<underlined>", "&n")
                   .replace("<strikethrough>", "&m")
                   .replace("<obfuscated>", "&k")
                   .replace("<reset>", "&r")
                   .replace("<black>", "&0")
                   .replace("<dark_blue>", "&1")
                   .replace("<dark_green>", "&2")
                   .replace("<dark_aqua>", "&3")
                   .replace("<dark_red>", "&4")
                   .replace("<dark_purple>", "&5")
                   .replace("<gold>", "&6")
                   .replace("<gray>", "&7")
                   .replace("<dark_gray>", "&8")
                   .replace("<blue>", "&9")
                   .replace("<green>", "&a")
                   .replace("<aqua>", "&b")
                   .replace("<red>", "&c")
                   .replace("<light_purple>", "&d")
                   .replace("<yellow>", "&e")
                   .replace("<white>", "&f");

        // Convert hex colors &#RRGGBB to ChatColor
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
