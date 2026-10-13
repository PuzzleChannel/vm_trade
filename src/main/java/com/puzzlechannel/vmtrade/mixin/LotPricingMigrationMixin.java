package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.onlyjugs.block.TradeChestBlockEntity;
import net.onlyjugs.data.Listing;
import com.puzzlechannel.vmtrade.LotPricingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One-time compatibility migration from per-item sell prices to whole-lot
 * prices. This custom marker is independent of Open Trade's own storage version,
 * so each existing chest is converted exactly once when its chunk is loaded.
 */
@Mixin(targets = "net.onlyjugs.block.TradeChestBlockEntity")
public abstract class LotPricingMigrationMixin {
    private static final String VMTRADE_PRICE_VERSION_KEY = "VmTradeLotPricingVersion";
    private static final int VMTRADE_PRICE_VERSION = 1;

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false, require = 1)
    private void vmtrade$migrateExistingLotPrices(ValueInput input, CallbackInfo ci) {
        if (input.getIntOr(VMTRADE_PRICE_VERSION_KEY, 0) >= VMTRADE_PRICE_VERSION) return;

        TradeChestBlockEntity chest = (TradeChestBlockEntity) (Object) this;
        for (int slot = 0; slot < 27; slot++) {
            Listing listing = chest.getListing(slot);
            if (listing == null) continue;

            ItemStack stack = chest.getItem(slot);
            if (stack == null || stack.isEmpty() || stack.getCount() <= 1) continue;

            int oldPerItemPrice = listing.unitPrice();
            if (oldPerItemPrice <= 0) continue;

            int migratedPrice = LotPricingState.migrateLegacyUnitPrice(oldPerItemPrice, stack.getCount());
            chest.setListing(slot, Listing.forSale(migratedPrice));
        }

        // Persist the marker even when this chest had no multi-item listings;
        // otherwise a newly created listing could be migrated again on a later save.
        chest.setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false, require = 1)
    private void vmtrade$writeLotPricingVersion(ValueOutput output, CallbackInfo ci) {
        output.putInt(VMTRADE_PRICE_VERSION_KEY, VMTRADE_PRICE_VERSION);
    }
}
