package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.onlyjugs.menu.TradeChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Returns a cancelled sell listing to the player's inventory immediately.
 * The original menu puts the item into its temporary stage slot and normally
 * moves it into the inventory only when the menu closes. We consume that stage
 * stack immediately and then broadcast the menu state so the visible player
 * inventory updates without closing the screen.
 */
@Mixin(targets = "net.onlyjugs.net.OpenTradeNet")
public abstract class CancelListingReturnMixin {
    @Inject(method = "handleCancelStageListing", at = @At("TAIL"), require = 1, remap = false)
    private static void vmtrade$returnCancelledListing(
            net.onlyjugs.net.CancelStageListingPayload payload,
            ServerPlayer player,
            CallbackInfo ci) {
        if (!(player.containerMenu instanceof TradeChestMenu menu)) {
            return;
        }

        ItemStack staged = menu.stageStack();
        if (staged == null || staged.isEmpty()) {
            return;
        }

        ItemStack returned = staged.copy();
        menu.stageContainer().setItem(0, ItemStack.EMPTY);

        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }

        menu.broadcastChanges();
    }
}
