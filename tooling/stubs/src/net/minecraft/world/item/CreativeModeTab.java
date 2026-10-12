package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CreativeModeTab {
    public interface Output {
        void accept(ItemStack item);
    }

    public static class Builder {
        public Builder icon(Supplier<ItemStack> icon) { return this; }
        public Builder title(Component title) { return this; }
        public Builder displayItems(BiConsumer<Object, Output> consumer) { return this; }
        public CreativeModeTab build() { return new CreativeModeTab(); }
    }

    public static Builder builder() { return new Builder(); }
}
