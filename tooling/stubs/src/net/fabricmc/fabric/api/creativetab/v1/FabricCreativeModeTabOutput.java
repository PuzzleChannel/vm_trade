package net.fabricmc.fabric.api.creativetab.v1;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
public class FabricCreativeModeTabOutput implements CreativeModeTab.Output {
    @Override public void accept(ItemStack stack) {}
    public void prepend(ItemStack stack) {}
    public void prepend(ItemLike item) {}
}
