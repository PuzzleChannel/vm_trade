package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small client-side polish for Open Trade's original Trade Chest screen.
 * All layout changes are applied to live widgets; vendor classes are not patched.
 */
@Mixin(targets = "net.onlyjugs.client.screen.TradeChestScreen")
public abstract class TradeChestScreenPolishMixin {
    private static final Pattern BALANCE_SUFFIX = Pattern.compile(
            "^(.*\\d.*?)\\s+(?:ВК|ВМуткоин\\s*\\(ВК\\)|VC|VMCoin(?:\\s*\\(VC\\))?)$"
    );

    @Inject(method = "init", at = @At("TAIL"), remap = false, require = 1)
    private void vmtrade$centerListingControls(CallbackInfo ci) {
        try {
            Object price = readField(this, "priceField");
            Object list = readField(this, "listButton");
            Object cancel = readField(this, "cancelListingButton");
            Object cancelBuyOrder = readField(this, "cancelBuyOrderButton");
            if (price == null || list == null || cancel == null) {
                return;
            }

            int left = readIntField(this, "leftPos");
            int panelWidth = readIntField(this, "imageWidth");
            int priceWidth = 26; // Fits three Minecraft-font digits and the caret.
            int listWidth = readIntField(list, "width");
            int cancelWidth = readIntField(cancel, "width");
            int gap = 2;
            int controlsWidth = priceWidth + gap + listWidth + gap + cancelWidth;
            int startX = left + (panelWidth - controlsWidth) / 2;
            int rowY = readIntField(price, "y") + 12;

            setWidgetBounds(price, startX, rowY, priceWidth);
            setWidgetBounds(list, startX + priceWidth + gap, rowY, listWidth);
            setWidgetBounds(cancel, startX + priceWidth + gap + listWidth + gap, rowY, cancelWidth);

            // The buy-order cancel control is mutually exclusive with the sell controls.
            if (cancelBuyOrder != null) {
                int buyWidth = readIntField(cancelBuyOrder, "width");
                setWidgetBounds(cancelBuyOrder, left + (panelWidth - buyWidth) / 2, rowY, buyWidth);
            }

            configurePriceInput(price);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Layout is cosmetic; never block the trade chest from opening.
            System.err.println("[VM Trade] Could not center listing controls: " + exception.getMessage());
        }
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private void vmtrade$hideTitleAndRenderCoinBalance(
            @Coerce Object graphics,
            @Coerce Object font,
            Component component,
            int x,
            int y,
            int color,
            boolean shadow
    ) {
        String text = component == null ? "" : component.getString();
        if ("Trade Chest".equals(text) || "Торговый сундук".equals(text)) {
            return;
        }

        Matcher balance = BALANCE_SUFFIX.matcher(text);
        if (balance.matches()) {
            String amount = balance.group(1).trim();
            try {
                int textWidth = fontWidth(font, amount);
                int left = readIntField(this, "leftPos");
                int panelWidth = readIntField(this, "imageWidth");
                int groupWidth = textWidth + 13; // Text plus the 16px coin texture, with the existing 3px overlap.
                int centeredX = left + (panelWidth - groupWidth) / 2;

                if (drawBalanceWithCoin(graphics, font, amount, centeredX, y + 12, color, shadow)) {
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                System.err.println("[VM Trade] Could not render wallet coin icon: " + exception.getMessage());
            }
        }

        drawOriginalText(graphics, font, component, x, y, color, shadow);
    }

    private static void configurePriceInput(Object price) throws ReflectiveOperationException {
        Method maxLength = price.getClass().getMethod("setMaxLength", int.class);
        maxLength.invoke(price, 3);

        Predicate<String> digitsOnly = value -> value != null && value.matches("[0-9]{0,3}");
        for (Class<?> type = price.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!"setFilter".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter.isAssignableFrom(Predicate.class) || Predicate.class.isAssignableFrom(parameter)) {
                    method.setAccessible(true);
                    method.invoke(price, digitsOnly);
                    return;
                }
            }
        }
        throw new NoSuchMethodException("EditBox.setFilter(Predicate)");
    }

    private static boolean drawBalanceWithCoin(
            Object graphics, Object font, String amount, int x, int y, int color, boolean shadow
    ) throws ReflectiveOperationException {
        Object label = Component.literal(amount);
        Class<?> hooks = Class.forName("com.puzzlechannel.vmtrade.VmTradeUiHooks");
        for (Method method : hooks.getMethods()) {
            if (!"drawBalance".equals(method.getName()) || method.getParameterCount() != 7) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters[0].isInstance(graphics)
                    && parameters[1].isInstance(font)
                    && parameters[2].isInstance(label)) {
                method.invoke(null, graphics, font, label, x, y, color, shadow);
                return true;
            }
        }
        return false;
    }

    private static void drawOriginalText(
            Object graphics, Object font, Component component, int x, int y, int color, boolean shadow
    ) {
        try {
            for (Method method : graphics.getClass().getMethods()) {
                if (!"text".equals(method.getName()) || method.getParameterCount() != 6) {
                    continue;
                }
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters[0].isInstance(font)
                        && parameters[1].isInstance(component)
                        && parameters[2] == int.class
                        && parameters[3] == int.class
                        && parameters[4] == int.class
                        && parameters[5] == boolean.class) {
                    method.invoke(graphics, font, component, x, y, color, shadow);
                    return;
                }
            }
            throw new NoSuchMethodException("GuiGraphicsExtractor.text(Font, Component, int, int, int, boolean)");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            System.err.println("[VM Trade] Could not render screen text: " + exception.getMessage());
        }
    }

    private static int fontWidth(Object font, String text) throws ReflectiveOperationException {
        Method width = font.getClass().getMethod("width", String.class);
        return ((Number) width.invoke(font, text)).intValue();
    }

    private static void setWidgetBounds(Object widget, int x, int y, int width)
            throws ReflectiveOperationException {
        writeIntField(widget, "x", x);
        writeIntField(widget, "y", y);
        writeIntField(widget, "width", width);
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
                // The widget coordinates live on AbstractWidget; screen fields are inherited too.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
