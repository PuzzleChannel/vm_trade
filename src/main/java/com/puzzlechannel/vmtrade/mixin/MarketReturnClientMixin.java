package com.puzzlechannel.vmtrade.mixin;

import net.onlyjugs.client.ItemDetailCache;
import net.onlyjugs.net.MarketResultsPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A MarketResultsPayload is the authoritative signal that the client has
 * returned to the market listing. Clear the cached detail model before the
 * original receiver stores the listing results, otherwise MarketScreen keeps
 * rendering the stale detail view even though the server cleared its detail
 * selection.
 */
@Mixin(targets = "net.onlyjugs.net.OpenTradeNet")
public abstract class MarketReturnClientMixin {
    @Inject(
            method = "lambda$registerClient$1",
            at = @At("HEAD"),
            require = 1,
            remap = false
    )
    private static void vmtrade$clearDetailWhenMarketResultsArrive(
            MarketResultsPayload payload,
            CallbackInfo ci
    ) {
        ItemDetailCache.clear();
    }
}
