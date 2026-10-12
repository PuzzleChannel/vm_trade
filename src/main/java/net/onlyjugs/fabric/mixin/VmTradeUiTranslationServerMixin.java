package net.onlyjugs.fabric.mixin;

import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * VM Trade's uniquely named runtime translation hook for server/player-facing
 * Open Trade messages. A unique class name prevents collisions with stale
 * classes that may be supplied by other Open Trade-derived mods.
 */
@Mixin(targets = {
        "net.onlyjugs.net.OpenTradeNet",
        "net.onlyjugs.net.OpenTradeNet$1",
        "net.onlyjugs.net.OpenTradeNet$2",
        "net.onlyjugs.net.OpenTradeNet$3",
        "net.onlyjugs.net.OpenTradeNet$4",
        "net.onlyjugs.OpenTrade"
})
public abstract class VmTradeUiTranslationServerMixin {
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
