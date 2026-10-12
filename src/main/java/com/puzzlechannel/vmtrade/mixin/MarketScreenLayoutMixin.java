package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Tiny runtime-only layout adjustment for the two right-panel section headings.
 * It changes only the Y argument of the text calls for those exact labels.
 */
@Mixin(targets = "net.onlyjugs.client.screen.MarketScreen")
public abstract class MarketScreenLayoutMixin {
    @ModifyArgs(
            method = "renderRightPanel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
            ),
            require = 0
    )
    private void vmtrade$moveMarketHeadingsDown(Args args) {
        Object value = args.get(1);
        if (!(value instanceof Component component)) {
            return;
        }

        String text = component.getString();
        if ("Sell Orders".equals(text)
                || "Recent Trades".equals(text)
                || "Заявки на продажу".equals(text)
                || "Последние сделки".equals(text)) {
            args.set(3, ((Integer) args.get(3)) + 2);
        }
    }
}
