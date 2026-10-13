package net.minecraft.world.item;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack();
    public ItemStack() {}
    public ItemStack(Item item) {}
    public ItemStack copy() { return this; }
    public boolean isEmpty() { return true; }
    public int getCount() { return 1; }
    public void setCount(int count) {}
    public void shrink(int count) {}
    public int getMaxStackSize() { return 64; }
}
