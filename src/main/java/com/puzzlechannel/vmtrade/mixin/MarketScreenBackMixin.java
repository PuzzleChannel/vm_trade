package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.onlyjugs.net.OpenMarketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The detail screen's Back callback must reopen the market root, not the trade chest. */
@Mixin(targets = "net.onlyjugs.client.screen.MarketScreen")
public abstract class MarketScreenBackMixin {
    @ModifyArg(
            method = "lambda$buildOurWidgets$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/onlyjugs/platform/NetworkServices;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
                    remap = false),
            index = 0,
            require = 1,
            remap = false
    )
    private static CustomPacketPayload vmtrade$returnToMarketHome(CustomPacketPayload ignored) {
        return new OpenMarketPayload("");
    }
}
