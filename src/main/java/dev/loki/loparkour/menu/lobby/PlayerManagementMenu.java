package dev.loki.loparkour.menu.lobby;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.mode.base.Modes;
import dev.loki.loparkour.player.core.ParkourPlayer;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerManagementMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        ParkourPlayer viewer = ParkourPlayer.getPlayer(player);
        if (viewer == null) return;

        var session = viewer.session;
        String locale = viewer.locale;
        String title = Locales.getString(locale, "lobby.player_management.name");

        List<ParkourUser> others = new ArrayList<>();
        others.addAll(session.getPlayers().stream().filter(p -> p != viewer).toList());
        others.addAll(session.getSpectators());

        InventoryGUI gui = baseGui(title, 3)
                .setItem(22, closeButton(player), e -> Menus.LOBBY.open(player));

        for (int i = 0; i < Math.min(others.size(), 7); i++) {
            final int idx = i;
            final ParkourUser other = others.get(i);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (!ParkourUser.isBedrockPlayer(other.player)
                    && other.getName() != null && !other.getName().startsWith(".")) {
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(other.player);
                    skull.setItemMeta(meta);
                }
            }

            boolean muted = session.isMuted(other);
            ItemMeta displayMeta = skull.getItemMeta();
            if (displayMeta != null) {
                String muteSuffix = muted
                        ? Locales.getString(locale, "lobby.player_management.head.muted_suffix")
                        : "";
                displayMeta.setDisplayName(ColorUtil.color(
                        Locales.getString(locale, "lobby.player_management.head.name")
                                .formatted(other.getName(), muteSuffix)));
                String muteLoreKey = muted
                        ? "lobby.player_management.head.lore_unmute"
                        : "lobby.player_management.head.lore_mute";
                displayMeta.setLore(List.of(
                        ColorUtil.color(Locales.getString(locale, "lobby.player_management.head.lore_kick")),
                        ColorUtil.color(Locales.getString(locale, muteLoreKey))
                ));
                skull.setItemMeta(displayMeta);
            }

            gui = gui.setItem(10 + idx, skull, e -> {
                if (e.getClick() == ClickType.LEFT) {
                    Modes.DEFAULT.create(other.player);
                    other.sendTranslated("lobby.player_management.kicked");
                    viewer.sendTranslated("lobby.player_management.advice");
                } else if (e.getClick() == ClickType.RIGHT) {
                    session.toggleMute(other);
                    boolean nowMuted = session.isMuted(other);
                    other.sendTranslated(nowMuted ? "lobby.player_management.muted" : "lobby.player_management.unmuted");
                }
                open(player);
            });
        }

        gui.open(player);
    }
}
