package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Set;

public class BlockEntityType<T extends BlockEntity> {
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    public BlockEntityType(BlockEntitySupplier<? extends T> supplier, Set<Block> validBlocks) {}
    public boolean isValid(Block block) { return true; }
}
