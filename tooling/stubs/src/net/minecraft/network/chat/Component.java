package net.minecraft.network.chat;

public interface Component {
    static MutableComponent literal(String s) { return new Impl(); }
    static MutableComponent translatable(String s) { return new Impl(); }

    final class Impl implements MutableComponent {
    }
}
