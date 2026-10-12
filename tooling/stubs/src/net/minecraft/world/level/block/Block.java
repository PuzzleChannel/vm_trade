package net.minecraft.world.level.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;

public class Block extends BlockBehaviour {
    protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    public Block(BlockBehaviour.Properties p) {}
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h){return InteractionResult.PASS;}
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state){return null;}
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type){return null;}
}
