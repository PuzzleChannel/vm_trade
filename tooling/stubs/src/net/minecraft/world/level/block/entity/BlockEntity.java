package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntity {
    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {}
    public BlockEntityType<?> getType() { return null; }
}
