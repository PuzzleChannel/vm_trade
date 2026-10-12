package net.minecraft.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface MenuProvider {
    AbstractContainerMenu createMenu(int id, Inventory inventory, Player player);
    Component getDisplayName();
}
