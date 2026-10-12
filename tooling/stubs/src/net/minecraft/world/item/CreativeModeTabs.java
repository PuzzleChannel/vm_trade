package net.minecraft.world.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class CreativeModeTabs {
    public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "building_blocks"));
    public static final ResourceKey<CreativeModeTab> INGREDIENTS =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "ingredients"));

    private CreativeModeTabs() {}
}
