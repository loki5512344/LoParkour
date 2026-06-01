package dev.loki.loparkour.menu.lobby;

import dev.loki.loparkour.config.locale.Locales;
import dev.loki.loparkour.menu.core.LPMenu;
import dev.loki.loparkour.menu.core.Menus;
import dev.loki.loparkour.menu.core.ParkourOption;
import dev.loki.loparkour.player.core.ParkourUser;
import dev.loki.loparkour.session.core.Session;
import dev.loki.loparkour.util.text.ColorUtil;
import dev.lolib.gui.InventoryGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LobbyMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        if (user == null) return;

        String locale = user.locale;
        String title = Locales.getString(locale, "lobby.name");

        boolean isHost = !user.session.getPlayers().isEmpty()
                && user.session.getPlayers().get(0) == user;

        InventoryGUI gui = baseGui(title, 3);

        if (ParkourOption.PLAYER_MANAGEMENT.mayPerform(player) && isHost) {
            gui = gui.setItem(11, localeItem(player, "lobby.player_management.item"),
                    e -> Menus.PLAYER_MANAGEMENT.open(player));
        }

        if (ParkourOption.VISIBILITY.mayPerform(player) && isHost) {
            gui = gui.setItem(13, visibilityItem(user), e -> {
                Session.Visibility next = switch (user.session.getVisibility()) {
                    case PUBLIC  -> Session.Visibility.ID_ONLY;
                    case ID_ONLY -> Session.Visibility.PRIVATE;
                    case PRIVATE -> Session.Visibility.PUBLIC;
                };
                user.session.setVisibility(next);
                open(player);
            });
        }

        gui.setItem(22, closeButton(player), e -> player.closeInventory())
           .open(player);
    }

    private ItemStack visibilityItem(ParkourUser user) {
        String locale = user.locale;
        List<String> values = Locales.getStringList(locale, "lobby.visibility.values");
        String label = switch (user.session.getVisibility()) {
            case PUBLIC  -> values.size() > 2 ? values.get(2) : "Public";
            case ID_ONLY -> values.size() > 1 ? values.get(1) : "ID Only";
            case PRIVATE -> !values.isEmpty() ? values.get(0) : "Private";
        };
        Material mat = switch (user.session.getVisibility()) {
            case PUBLIC  -> Material.LIME_STAINED_GLASS_PANE;
            case ID_ONLY -> Material.YELLOW_STAINED_GLASS_PANE;
            case PRIVATE -> Material.RED_STAINED_GLASS_PANE;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String visibilityName = Locales.getString(locale, "lobby.visibility.name");
            meta.setDisplayName(ColorUtil.color(visibilityName + " §7» §f" + label));
            item.setItemMeta(meta);
        }
        return item;
    }
}
