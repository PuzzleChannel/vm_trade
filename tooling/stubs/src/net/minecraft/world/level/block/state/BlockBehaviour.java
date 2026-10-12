package net.minecraft.world.level.block.state;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;

public class BlockBehaviour {
    public static class Properties {
        public static Properties of() { return new Properties(); }
        public static Properties ofFullCopy(BlockBehaviour block) { return new Properties(); }
        public Properties setId(ResourceKey<Block> k) { return this; }
        public Properties strength(float f) { return this; }
        public Properties strength(float destroyTime, float explosionResistance) { return this; }
        public Properties pushReaction(PushReaction reaction) { return this; }
    }
}
