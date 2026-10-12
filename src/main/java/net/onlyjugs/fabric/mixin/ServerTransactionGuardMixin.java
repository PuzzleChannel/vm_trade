package net.onlyjugs.fabric.mixin;

import com.puzzlechannel.vmtrade.TransactionGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fail-closed validation and replay throttling before economy-changing handlers run. */
@Mixin(targets = "net.onlyjugs.net.OpenTradeNet")
public abstract class ServerTransactionGuardMixin {
    private static final int MAX_PRICE_CENTS = 1_000_000_000;
    private static final int MAX_BULK_QUANTITY = 4096;

    @Inject(method = "handleBuy", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardBuy(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        if (!validItemKey(stringValue(payload, "itemKey")) || !validPrice(intValue(payload, "unitPrice"))
                || intValue(payload, "quantity") < 0 || intValue(payload, "quantity") > MAX_BULK_QUANTITY) {
            ci.cancel(); return;
        }
        guard(player, "buy-listing", payload, ci);
    }

    @Inject(method = "handleBuyUniqueStack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardUniqueBuy(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        if (value(payload, "chestPos") == null || intValue(payload, "slot") < 0 || intValue(payload, "slot") >= 27
                || !validPrice(intValue(payload, "expectedUnitPrice"))
                || intValue(payload, "requestedQty") < 1 || intValue(payload, "requestedQty") > 1000) {
            ci.cancel(); return;
        }
        guard(player, "buy-unique", payload, ci);
    }

    @Inject(method = "handleWallet", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardWallet(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        int amount = intValue(payload, "amount");
        if (amount < -1_000_000 || amount > 1_000_000) { ci.cancel(); return; }
        guard(player, "wallet", payload, ci);
    }

    @Inject(method = "handlePlaceBuyOrder", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardPlaceOrder(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        if (!validItemKey(stringValue(payload, "itemKey")) || !validPrice(intValue(payload, "unitPrice"))
                || intValue(payload, "quantity") < 1 || intValue(payload, "quantity") > MAX_BULK_QUANTITY) {
            ci.cancel(); return;
        }
        guard(player, "place-buy-order", payload, ci);
    }

    @Inject(method = "handleCancelBuyOrder", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardCancelOrder(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        if (value(payload, "orderId") == null) { ci.cancel(); return; }
        guard(player, "cancel-buy-order", payload, ci);
    }

    @Inject(method = "handleVillagerExecute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardVillagerTrade(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        if (intValue(payload, "entityId") < 0 || intValue(payload, "offerIndex") < 0
                || intValue(payload, "offerIndex") > 1024 || intValue(payload, "quantity") < 1
                || intValue(payload, "quantity") > 64) { ci.cancel(); return; }
        guard(player, "villager-trade", payload, ci);
    }

    @Inject(method = "handleSetListing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardSetListing(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        int price = intValue(payload, "price");
        if (intValue(payload, "slot") < 0 || intValue(payload, "slot") >= 27 || price < 0
                || price > MAX_PRICE_CENTS || (price > 0 && price < 3)) { ci.cancel(); return; }
        guard(player, "set-listing", payload, ci);
    }

    @Inject(method = "handleListStage", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardListStage(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        int price = intValue(payload, "priceCents");
        if (price < 3 || price > MAX_PRICE_CENTS) { ci.cancel(); return; }
        guard(player, "list-stage", payload, ci);
    }

    @Inject(method = "handleCancelStageListing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vmtrade$guardCancelStage(@Coerce Object payload, ServerPlayer player, CallbackInfo ci) {
        guard(player, "cancel-stage-listing", payload, ci);
    }

    private static void guard(ServerPlayer player, String operation, Object payload, CallbackInfo ci) {
        if (!TransactionGuard.allow(player, operation, payload)) ci.cancel();
    }

    private static Object value(Object payload, String accessor) {
        if (payload == null) return null;
        try { return payload.getClass().getMethod(accessor).invoke(payload); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
    }

    private static int intValue(Object payload, String accessor) {
        Object result = value(payload, accessor);
        return result instanceof Number number ? number.intValue() : Integer.MIN_VALUE;
    }

    private static String stringValue(Object payload, String accessor) {
        Object result = value(payload, accessor);
        return result instanceof String text ? text : null;
    }

    private static boolean validPrice(int price) {
        return price > 0 && price <= MAX_PRICE_CENTS;
    }

    private static boolean validItemKey(String key) {
        return key != null && !key.isBlank() && key.length() <= 256;
    }
}
