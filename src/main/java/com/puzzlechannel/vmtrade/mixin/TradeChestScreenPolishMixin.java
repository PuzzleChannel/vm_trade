package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
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

            // Keep the sell slot, price input, and action buttons as one centered row.
            // The slot's matching texture frame is moved by the same x offset in the release assets.
            int slotWidth = 16;
            int priceWidth = 26; // Three digits fit without the old oversized field.
            int listWidth = Math.max(readIntField(list, "width"), 48);
            int cancelWidth = Math.max(readIntField(cancel, "width"), 52);
            int gap = 2;
            int controlsWidth = slotWidth + gap + priceWidth + gap + listWidth + gap + cancelWidth;
            int slotX = (panelWidth - controlsWidth) / 2;

            // StageSlot is the actual clickable/input slot in the menu. Its screen
            // position must match the shifted 16x16 well in trade_chest.png.
            Object menu = readField(this, "menu");
            Object stageSlot = readField(menu, "stageSlot");
            if (stageSlot == null) {
                throw new NoSuchFieldException("menu.stageSlot");
            }
            writeIntField(stageSlot, "x", slotX);

            int rowY = readIntField(price, "y");
            int priceX = slotX + slotWidth + gap;
            int listX = priceX + priceWidth + gap;
            int cancelX = listX + listWidth + gap;

            setWidgetBounds(price, left + priceX, rowY, priceWidth);
            setWidgetBounds(list, left + listX, rowY, listWidth);
            setWidgetBounds(cancel, left + cancelX, rowY, cancelWidth);

            // The buy-order cancel control is mutually exclusive with sell controls.
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
            GuiGraphicsExtractor graphics,
            Font font,
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

        // Tell sellers that this field is the total price for the complete stack.
        try {
            Class<?> tooltipClass = Class.forName("net.minecraft.client.gui.components.Tooltip");
            Method createTooltip = tooltipClass.getMethod("create", Component.class);
            Object tooltip = createTooltip.invoke(null,
                    Component.literal("Цена указана за весь стак, не за одну штуку."));
            for (Method method : price.getClass().getMethods()) {
                if ("setTooltip".equals(method.getName()) && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(tooltip)) {
                    method.invoke(price, tooltip);
                    break;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Tooltip is a convenience; price input still works if the GUI API changes.
        }

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
