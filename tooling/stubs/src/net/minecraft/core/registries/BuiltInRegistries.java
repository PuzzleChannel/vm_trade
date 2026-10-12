package net.minecraft.core.registries;

import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BuiltInRegistries {
    public static Registry<Item> ITEM;
    public static Registry<Block> BLOCK;
    public static Registry<CreativeModeTab> CREATIVE_MODE_TAB;
    public static Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE;
}
