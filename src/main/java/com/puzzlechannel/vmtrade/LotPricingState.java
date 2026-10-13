package com.puzzlechannel.vmtrade;

import net.minecraft.server.level.ServerPlayer;
import net.onlyjugs.net.BuyUniqueStackPayload;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Runtime state for whole-lot purchases.
 *
 * Open Trade stores a listing's price in a field named unitPrice, but VM Trade
 * now interprets that amount as the total price for the complete stack.
 */
public final class LotPricingState {
    private static final ThreadLocal<Integer> ACTIVE_LOT_PRICE = new ThreadLocal<>();

    private LotPricingState() {}

    public static boolean beginUniquePurchase(BuyUniqueStackPayload payload, ServerPlayer player) {
        ACTIVE_LOT_PRICE.remove();
        if (payload == null || player == null) return false;

        final int slot;
        final int requested;
        final int expectedPrice;
        try {
            slot = payload.slot();
            requested = payload.requestedQty();
            expectedPrice = payload.expectedUnitPrice();

            Object chestPos = payload.chestPos();
            if (chestPos == null) return false;

            Object dimension = call(chestPos, "dimension");
            Object blockPos = call(chestPos, "pos");
            Object server = field(player, "server");
            Object level = callCompatible(server, "getLevel", dimension);
            if (level == null) return false;

            Object blockEntity = callCompatible(level, "getBlockEntity", blockPos);
            if (blockEntity == null
                    || !isInstanceOf(blockEntity, "net.onlyjugs.block.TradeChestBlockEntity")) {
                return false;
            }

            Object stack = callCompatible(blockEntity, "getItem", slot);
            if (stack == null) return false;
            int actualCount = ((Number) call(stack, "getCount")).intValue();

            // Only the complete current stack can be purchased at the listed lot price.
            if (actualCount < 1 || requested != actualCount) return false;

            Object listing = callCompatible(blockEntity, "getListing", slot);
            if (listing == null) {
                // Let the original handler report that the listing has disappeared.
                return true;
            }
            int currentPrice = ((Number) call(listing, "unitPrice")).intValue();
            if (currentPrice != expectedPrice || expectedPrice < 1) {
                // Let the original handler report that the listing price changed.
                return true;
            }

            ACTIVE_LOT_PRICE.set(expectedPrice);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ACTIVE_LOT_PRICE.remove();
            return false;
        }
    }

    public static boolean isActive() {
        return ACTIVE_LOT_PRICE.get() != null;
    }

    public static long priceForPayment(long calculatedPerItemTotal) {
        Integer lotPrice = ACTIVE_LOT_PRICE.get();
        return paymentAmount(lotPrice == null ? 0L : lotPrice.longValue(),
                calculatedPerItemTotal, lotPrice != null);
    }

    static long paymentAmount(long lotPrice, long calculatedPerItemTotal, boolean wholeLot) {
        return wholeLot ? lotPrice : calculatedPerItemTotal;
    }

    public static long sellerNetForPayment(long calculatedPerItemTotal) {
        long totalPrice = priceForPayment(calculatedPerItemTotal);
        try {
            Class<?> network = Class.forName("net.onlyjugs.net.OpenTradeNet");
            Method sellerNet = network.getDeclaredMethod("sellerNet", long.class);
            sellerNet.setAccessible(true);
            return ((Number) sellerNet.invoke(null, totalPrice)).longValue();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Unable to calculate seller proceeds for a whole-stack listing", exception);
        }
    }

    public static long affordableQuantity(long requestedQuantity, long affordableByBalance) {
        return affordableQuantity(requestedQuantity, affordableByBalance, isActive());
    }

    static long affordableQuantity(long requestedQuantity, long affordableByBalance, boolean wholeLot) {
        if (!wholeLot) return Math.min(requestedQuantity, affordableByBalance);
        // The full lot is affordable as soon as the balance covers its one total price.
        return affordableByBalance > 0 ? requestedQuantity : 0L;
    }

    /**
     * TradeHistory is an older per-item chart. Preserve its units by recording
     * the nearest representable cents-per-item for a whole-lot transaction.
     * This affects history display only; actual payment remains the full lot price.
     */
    public static int historyUnitPrice(int listedPrice, int quantity) {
        return historyUnitPrice(listedPrice, quantity, isActive());
    }

    static int historyUnitPrice(int listedPrice, int quantity, boolean wholeLot) {
        if (!wholeLot || quantity <= 1 || listedPrice <= 0) return listedPrice;
        long rounded = (listedPrice + (long) quantity / 2L) / quantity;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, rounded));
    }

    public static void clear() {
        ACTIVE_LOT_PRICE.remove();
    }

    private static boolean isInstanceOf(Object instance, String className) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(className)) return true;
        }
        return false;
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Continue searching inherited fields.
            }
        }
        throw new NoSuchFieldException(target.getClass().getName() + "." + name);
    }

    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object callCompatible(Object target, String methodName, Object argument)
            throws ReflectiveOperationException {
        if (target == null || argument == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                if (!accepts(parameter, argument)) continue;
                method.setAccessible(true);
                return method.invoke(target, argument);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
    }

    private static boolean accepts(Class<?> parameter, Object value) {
        if (!parameter.isPrimitive()) return parameter.isInstance(value);
        return (parameter == int.class && value instanceof Integer)
                || (parameter == long.class && value instanceof Long)
                || (parameter == boolean.class && value instanceof Boolean);
    }
}
