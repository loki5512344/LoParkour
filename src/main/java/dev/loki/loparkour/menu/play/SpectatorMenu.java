package dev.loki.loparkour.menu.play;

import dev.loki.loparkour.config.Locales;
import dev.loki.loparkour.menu.LPMenu;
import dev.loki.loparkour.mode.Modes;
import dev.loki.loparkour.player.ParkourUser;
import dev.loki.loparkour.session.Session;
import dev.loki.loparkour.util.ColorUtil;
import dev.loki.loparkour.world.Divider;
import dev.lolib.gui.ScrollableGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SpectatorMenu extends LPMenu {

    @Override
    public void open(@NotNull Player player) {
        String locale = locale(player);
        ParkourUser user = ParkourUser.getUser(player);
        String title = Locales.getString(locale, "play.spectator.name");

        List<Session> sessions = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();

        for (Session session : Divider.sections.keySet()) {
            if (!session.isAcceptingSpectators()) continue;
            if (user != null && session == user.session) continue;
            if (session.getPlayers().isEmpty()) continue;

            var pp = session.getPlayers().get(0);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            if (!ParkourUser.isBedrockPlayer(player)
                    && pp.getName() != null && !pp.getName().startsWith(".")) {
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) { meta.setOwningPlayer(pp.player); skull.setItemMeta(meta); }
            }
            sessions.add(session);
            items.add(skull);
        }

        ScrollableGUI gui = ScrollableGUI.create()
                .title(ColorUtil.color(title))
                .rows(3)
                .scrollUpButton(backItem(), 0)
                .scrollDownButton(nextItem(), 8);

        for (int i = 0; i < items.size(); i++) {
            final Session session = sessions.get(i);
            gui = gui.addItem(items.get(i), e -> Modes.SPECTATOR.create(player, session));
        }

        gui.open(player);
    }
}
