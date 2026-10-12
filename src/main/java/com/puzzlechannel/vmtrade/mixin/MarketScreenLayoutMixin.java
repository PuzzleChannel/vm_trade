package com.puzzlechannel.vmtrade.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(targets = "net.onlyjugs.client.screen.MarketScreen") 
public abstract class MarketScreenLayoutMixin {
    private static final Pattern BALANCE = Pattern.compile(
            "^(?:Баланс:\\s*)?([0-9][0-9,._ ]*)\\s+(?:ВМуткоин\\s*\\(ВК\\)|ВК|VMCoin(?:\\s*\\(VC\\))?|VC)$"
    );

    @ModifyArgs(method = "renderRightPanel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
    ), require = 0)
    private void vmtrade$moveMarketHeadingsDown(Args args) {
        Object value = args.get(1);
        if (!(value instanceof Component component)) return;
        String text = component.getString();
        if ("Sell Orders".equals(text) || "Recent Trades".equals(text)
                || "Заявки на продажу".equals(text) || "Последние сделки".equals(text)) {
            args.set(3, ((Integer) args.get(3)) + 2);
        }
    }

    @Redirect(method = "extractRenderState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
    ), require = 0)
    private void vmtrade$renderBalanceWithCoin(
            GuiGraphicsExtractor graphics, Font font, Component component,
            int x, int y, int color, boolean shadow
    ) {
        String text = component == null ? "" : component.getString();
        Matcher matcher = BALANCE.matcher(text);
        if (matcher.matches()) {
            String amount = matcher.group(1).trim();
            try {
                Component label = Component.literal("Баланс: " + amount);
                Class<?> hooks = Class.forName("com.puzzlechannel.vmtrade.VmTradeUiHooks");
                for (Method method : hooks.getMethods()) {
                    if (!"drawBalance".equals(method.getName()) || method.getParameterCount() != 7) continue;
                    Class<?>[] p = method.getParameterTypes();
                    if (p[0].isInstance(graphics) && p[1].isInstance(font) && p[2].isInstance(label)) {
                        method.invoke(null, graphics, font, label, x, y, color, shadow);
                        return;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                System.err.println("[VM Trade] Could not render market balance coin: " + exception.getMessage());
            }
        }
        graphics.text(font, component, x, y, color, shadow);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void vmtrade$fitQuantityInput(CallbackInfo ci) {
        try {
            Field field = findField(getClass(), "offerQtyField");
            Object widget = field.get(this);
            if (widget == null) return;
            Field xField = findField(widget.getClass(), "x");
            Field widthField = findField(widget.getClass(), "width");
            xField.setInt(widget, xField.getInt(widget) - 12);
            widthField.setInt(widget, widthField.getInt(widget) + 12);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            System.err.println("[VM Trade] Could not widen quantity input: " + exception.getMessage());
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
