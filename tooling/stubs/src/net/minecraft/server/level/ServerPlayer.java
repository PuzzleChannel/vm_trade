package net.minecraft.server.level;

import java.util.OptionalInt;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

public class ServerPlayer extends Player {
    public OptionalInt openMenu(MenuProvider provider) { return OptionalInt.empty(); }
}
