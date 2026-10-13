package org.spongepowered.asm.mixin.injection.invoke.arg;

public abstract class Args {
    public abstract <T> T get(int index);
    public abstract void set(int index, Object value);
}
