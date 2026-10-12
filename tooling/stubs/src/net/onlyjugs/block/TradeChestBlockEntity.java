package net.onlyjugs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Compile-time ABI stub for the vendor class. */
public class TradeChestBlockEntity extends BlockEntity {
    public TradeChestBlockEntity(BlockPos pos, BlockState state) { super(null, pos, state); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, TradeChestBlockEntity entity) {}
}
