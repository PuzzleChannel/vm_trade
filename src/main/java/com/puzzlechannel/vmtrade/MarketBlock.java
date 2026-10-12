package com.puzzlechannel.vmtrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Physical VM Trade market block.
 *
 * It reuses Open Trade's proven chest/listing interaction, but supplies its own
 * block-entity type so the normal vanilla chest renderer is never selected for
 * a Market. The server tick is forwarded to the original TradeChestBlockEntity.
 */
public final class MarketBlock extends net.onlyjugs.block.TradeChestBlock {
    public MarketBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // TradeChestBlock extends ChestBlock, whose default rendering is an animated chest.
        // The Market is a normal static model: never invoke the chest renderer.
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarketBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            // No renderer/ticker registration is needed client-side for this BE.
            return null;
        }
        if (type == VmTradeInitializer.MARKET_BE_TYPE) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<?>)
                    (lvl, pos, st, be) -> net.onlyjugs.block.TradeChestBlockEntity.serverTick(
                            lvl, pos, st, (net.onlyjugs.block.TradeChestBlockEntity) be);
        }
        return null;
    }
}
