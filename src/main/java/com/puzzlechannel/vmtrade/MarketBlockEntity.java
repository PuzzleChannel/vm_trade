package com.puzzlechannel.vmtrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Market storage entity with its own BlockEntityType. Having a distinct type is
 * intentional: the original Trade Chest uses the vanilla chest renderer, while
 * the Market must never render an opening chest over its own block model.
 */
public final class MarketBlockEntity extends net.onlyjugs.block.TradeChestBlockEntity {
    public MarketBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return VmTradeInitializer.MARKET_BE_TYPE;
    }
}
