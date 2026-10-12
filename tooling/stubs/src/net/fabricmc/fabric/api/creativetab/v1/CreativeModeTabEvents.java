package net.fabricmc.fabric.api.creativetab.v1;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeModeTabEvents {
    private CreativeModeTabEvents() {}

    @FunctionalInterface
    public interface ModifyOutput {
        void modifyOutput(FabricCreativeModeTabOutput output);
    }

    public static Event<ModifyOutput> modifyOutputEvent(ResourceKey<CreativeModeTab> resourceKey) {
        return new Event<>() {
            @Override
            public void register(ModifyOutput listener) {}
        };
    }
}
