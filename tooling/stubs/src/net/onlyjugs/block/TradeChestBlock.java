package net.onlyjugs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Compile-time ABI stub; the real class is supplied by the vendor Open Trade JAR at runtime. */
public class TradeChestBlock extends Block {
    public TradeChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return null; }
}
