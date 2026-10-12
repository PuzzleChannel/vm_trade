package net.minecraft.world.item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
public class Item implements ItemLike {
    public Item(Properties p){}
    public static class Properties {
        public Properties setId(ResourceKey<Item> k){return this;}
        public Properties stacksTo(int n){return this;}
        public Properties useBlockDescriptionPrefix(){return this;}
    }
}
