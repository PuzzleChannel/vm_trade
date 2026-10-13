package net.onlyjugs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onlyjugs.data.Listing;

/** Compile-time ABI stub for the vendor class; never included in releases. */
public class TradeChestBlockEntity extends BlockEntity {
    public TradeChestBlockEntity(BlockPos pos, BlockState state) { super(null, pos, state); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, TradeChestBlockEntity entity) {}
    public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    public Listing getListing(int slot) { return null; }
    public void setListing(int slot, Listing listing) {}
    public void setChanged() {}
}
