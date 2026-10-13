package net.minecraft.server.level;

import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

public class ServerPlayer extends Player {
    public OptionalInt openMenu(MenuProvider provider) { return OptionalInt.empty(); }
    public void sendSystemMessage(Component message, boolean overlay) {}
}
