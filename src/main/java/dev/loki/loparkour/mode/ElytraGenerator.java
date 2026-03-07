package dev.loki.loparkour.mode;

import dev.loki.loparkour.generator.ParkourGenerator;
import dev.loki.loparkour.session.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Generator for Elytra mode parkour
 */
public class ElytraGenerator extends ParkourGenerator {

    public ElytraGenerator(@NotNull Session session) {
        super(session);
    }

    @Override
    public Mode getMode() {
        return Modes.ELYTRA;
    }
}
