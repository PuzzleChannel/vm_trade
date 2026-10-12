package net.minecraft.world;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class SimpleMenuProvider implements MenuProvider {
    public interface MenuConstructor {
        AbstractContainerMenu create(int id, Inventory inventory, Player player);
    }
    public SimpleMenuProvider(MenuConstructor constructor, Component title) {}
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return null; }
    @Override public Component getDisplayName() { return null; }
}
