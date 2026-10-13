package com.puzzlechannel.vmtrade.mixin;

import com.puzzlechannel.vmtrade.LotPricingState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.onlyjugs.block.TradeChestBlockEntity;
import net.onlyjugs.data.BuyOrder;
import net.onlyjugs.data.BuyOrderBook;
import net.onlyjugs.net.OpenTradeNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies sell-listing prices to the complete listed stack.
 * Buy orders retain their existing per-item price semantics, so they must not
 * consume a multi-item lot using the old per-item transaction path.
 */
@Mixin(targets = "net.onlyjugs.net.OpenTradeNet")
public abstract class StackLotPricingMixin {
    @Inject(method = "handleBuyUniqueStack", at = @At("RETURN"), remap = false, require = 1)
    private static void vmtrade$clearLotPriceAfterPurchase(
            net.onlyjugs.net.BuyUniqueStackPayload payload, ServerPlayer player, CallbackInfo ci
    ) {
        LotPricingState.clear();
    }

    @Redirect(
            method = "handleBuyUniqueStack",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(JJ)J", remap = false),
            remap = false,
            require = 1
    )
    private static long vmtrade$checkBalanceAgainstLotPrice(long requestedQuantity, long affordableByBalance) {
        return LotPricingState.affordableQuantity(requestedQuantity, affordableByBalance);
    }

    @Redirect(
            method = "handleBuyUniqueStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/onlyjugs/data/WalletState;tryDebit(Ljava/util/UUID;J)Z",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static boolean vmtrade$debitTheWholeLotPrice(
            net.onlyjugs.data.WalletState wallet, java.util.UUID buyer, long calculatedPerItemTotal
    ) {
        return wallet.tryDebit(buyer, LotPricingState.priceForPayment(calculatedPerItemTotal));
    }

    @Redirect(
            method = "handleBuyUniqueStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/onlyjugs/net/OpenTradeNet;sellerNet(J)J",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static long vmtrade$calculateSellerProceedsFromLotPrice(long calculatedPerItemTotal) {
        return LotPricingState.sellerNetForPayment(calculatedPerItemTotal);
    }

    /**
     * The legacy aggregate-buy packet buys quantities at a per-item price. The
     * current UI uses BuyUniqueStackPayload; reject this old path to ensure it
     * cannot charge a lot price once per item.
     */
    @Inject(method = "handleBuy", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void vmtrade$rejectLegacyPerItemBuy(
            net.onlyjugs.net.BuyListingPayload payload, ServerPlayer player, CallbackInfo ci
    ) {
        player.sendSystemMessage(Component.literal(
                "§cЦена указана за весь стак. Откройте конкретный лот и купите его целиком."
        ), true);
        ci.cancel();
    }

    /**
     * Buy-order prices are still per item. Don't auto-match them against a
     * multi-item stack whose listing price is now a whole-lot price.
     */
    @Inject(
            method = "matchBuyOrdersAgainstListing",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private static void vmtrade$skipPerItemBuyOrdersForLot(
            ServerPlayer seller, TradeChestBlockEntity chest, int slot, CallbackInfo ci
    ) {
        if (slot < 0 || slot >= 27) {
            ci.cancel();
            return;
        }
        ItemStack stack = chest.getItem(slot);
        if (stack != null && !stack.isEmpty() && stack.getCount() > 1) {
            seller.sendSystemMessage(Component.literal(
                    "§eЗаявки на покупку оценивают штуки по отдельности и не исполняются автоматически для лота целиком."
            ), true);
            ci.cancel();
        }
    }

    /**
     * When a buyer posts an order, filter out multi-item sell lots from the
     * old per-item matcher. Single-item listings continue to match normally.
     */
    @Redirect(
            method = "matchOrderAgainstListings",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/onlyjugs/net/OpenTradeNet;findListings(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;I)Ljava/util/List;",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static List<?> vmtrade$onlyMatchBuyOrdersToSingleItems(
            ServerPlayer buyer, String itemKey, int minimumPrice
    ) {
        try {
            Method findListings = OpenTradeNet.class.getDeclaredMethod(
                    "findListings", ServerPlayer.class, String.class, int.class
            );
            findListings.setAccessible(true);
            Object found = findListings.invoke(null, buyer, itemKey, minimumPrice);
            if (!(found instanceof List<?> listings)) return List.of();

            List<Object> singleItemListings = new ArrayList<>(listings.size());
            for (Object listing : listings) {
                Object stack = recordValue(listing, "stack");
                if (stack instanceof ItemStack itemStack && !itemStack.isEmpty() && itemStack.getCount() == 1) {
                    singleItemListings.add(listing);
                }
            }
            return singleItemListings;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Fail closed: never use a per-item matcher when the listing shape
            // can't be inspected safely.
            return List.of();
        }
    }

    private static Object recordValue(Object target, String name) throws ReflectiveOperationException {
        Method accessor = target.getClass().getDeclaredMethod(name);
        accessor.setAccessible(true);
        return accessor.invoke(target);
    }
}
