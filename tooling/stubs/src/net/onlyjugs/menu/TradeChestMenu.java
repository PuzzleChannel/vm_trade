package net.onlyjugs.menu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
public class TradeChestMenu extends AbstractContainerMenu {
    public ItemStack stageStack() { return ItemStack.EMPTY; }
    public Container stageContainer() { return null; }
}
