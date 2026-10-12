package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Runtime-only translation hook for the original Open Trade client screens.
 * No vendor .class file is rewritten in the JAR; Mixin applies this redirect
 * only in memory when Minecraft loads the screen classes.
 */
@Mixin(targets = {
        "net.onlyjugs.client.screen.MarketScreen",
        "net.onlyjugs.client.screen.TradeChestScreen",
        "net.onlyjugs.client.screen.VillagerTradeScreen"
})
public abstract class UiTranslationClientMixin {
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
