package com.puzzlechannel.vmtrade;

import net.fabricmc.api.ClientModInitializer;

public final class VmTradeClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            Object original = Class.forName("net.onlyjugs.fabric.OpenTradeFabricClient")
                    .getConstructor().newInstance();
            original.getClass().getMethod("onInitializeClient").invoke(original);

            // Restore the previously shipped player wallet UI, Market/History tabs,
            // exact coin balance rendering, and the sales-history payload receiver.
            Class.forName("com.puzzlechannel.vmtrade.TradeHistoryNetwork")
                    .getMethod("registerClient").invoke(null);
            Class.forName("com.puzzlechannel.vmtrade.TradeChestClientUi")
                    .getMethod("install").invoke(null);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : e;
            throw new RuntimeException("Unable to initialize VM Trade client features", cause);
        }
    }
}
