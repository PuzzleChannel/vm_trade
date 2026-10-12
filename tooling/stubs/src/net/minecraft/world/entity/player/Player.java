package net.minecraft.world.entity.player;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;

public class Player {
    public AbstractContainerMenu containerMenu;
    public Inventory getInventory() { return new Inventory(); }
    public ItemEntity drop(ItemStack stack, boolean random) { return null; }
}
