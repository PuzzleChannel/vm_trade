package net.onlyjugs.fabric.mixin;

import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Runtime-only translation hook for server/player-facing Open Trade messages.
 * The pristine Open Trade bytecode remains unchanged on disk.
 */
@Mixin(targets = {
        "net.onlyjugs.net.OpenTradeNet",
        "net.onlyjugs.net.OpenTradeNet$1",
        "net.onlyjugs.net.OpenTradeNet$2",
        "net.onlyjugs.net.OpenTradeNet$3",
        "net.onlyjugs.net.OpenTradeNet$4",
        "net.onlyjugs.OpenTrade"
})
public abstract class UiTranslationServerMixin {
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0,
            expect = 0
    )
    private static MutableComponent vmtrade$translateLiteral(String text) {
        return com.puzzlechannel.vmtrade.UiTextTranslations.componentLiteral(text);
    }
}
