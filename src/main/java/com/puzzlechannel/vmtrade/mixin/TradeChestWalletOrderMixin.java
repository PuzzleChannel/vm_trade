package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Keeps the legacy wallet row in the requested order and wording:
 * amount field, then Withdraw, then Deposit. The underlying callbacks still
 * use Open Trade's signed WalletActionPayload and server-side wallet handler.
 */
@Mixin(targets = "com.puzzlechannel.vmtrade.TradeChestClientUi$UiState")
public abstract class TradeChestWalletOrderMixin {
    @Inject(method = "install", at = @At("TAIL"), remap = false, require = 1)
    private void vmtrade$orderWalletButtons(CallbackInfo ci) {
        try {
            Object deposit = readField(this, "deposit");
            Object withdraw = readField(this, "withdraw");
            if (deposit == null || withdraw == null) {
                return;
            }

            int depositX = readIntField(deposit, "x");
            int withdrawX = readIntField(withdraw, "x");
            writeIntField(deposit, "x", withdrawX);
            writeIntField(withdraw, "x", depositX);

            setMessage(deposit, "Пополнить");
            setMessage(withdraw, "Вывести");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // UI polish must never prevent the trading screen from opening.
            System.err.println("[VM Trade] Could not arrange wallet controls: " + exception.getMessage());
        }
    }

    private static void setMessage(Object widget, String message) throws ReflectiveOperationException {
        Method method = widget.getClass().getMethod("setMessage", Component.class);
        method.invoke(widget, Component.literal(message));
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        return findField(target.getClass(), name).get(target);
    }

    private static int readIntField(Object target, String name) throws ReflectiveOperationException {
        return findField(target.getClass(), name).getInt(target);
    }

    private static void writeIntField(Object target, String name, int value) throws ReflectiveOperationException {
        findField(target.getClass(), name).setInt(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Search superclasses for AbstractWidget's x coordinate.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
