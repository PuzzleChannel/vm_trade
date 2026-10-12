package net.minecraft.core;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface Registry<T> {
    static <T> T register(Registry<T> registry, ResourceKey<T> key, T value) {
        return value;
    }

    static <T> T register(Registry<T> registry, Identifier id, T value) {
        return value;
    }
}
