package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Isolated VM Trade translation hook for server/player-facing Open Trade messages.
 * This class deliberately uses the mod's own namespace to avoid class-loader
 * collisions with Open Trade-derived mods that share the upstream package.
 */
@Mixin(targets = {
        "net.onlyjugs.net.OpenTradeNet",
        "net.onlyjugs.net.OpenTradeNet$1",
        "net.onlyjugs.net.OpenTradeNet$2",
        "net.onlyjugs.net.OpenTradeNet$3",
        "net.onlyjugs.net.OpenTradeNet$4",
        "net.onlyjugs.OpenTrade",
        "com.puzzlechannel.vmtrade.TradeHistoryNetwork",
        "com.puzzlechannel.vmtrade.TradeSyncService"
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
