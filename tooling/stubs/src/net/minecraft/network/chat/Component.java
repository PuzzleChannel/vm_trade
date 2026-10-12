package net.minecraft.network.chat;

public interface Component {
    static MutableComponent literal(String s) { return new Impl(); }
    static MutableComponent translatable(String s) { return new Impl(); }
    default String getString() { return \"\"; }

    final class Impl implements MutableComponent {
    }
}
