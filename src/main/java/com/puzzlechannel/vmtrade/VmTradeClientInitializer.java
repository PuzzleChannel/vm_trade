package com.puzzlechannel.vmtrade;

import net.fabricmc.api.ClientModInitializer;

public final class VmTradeClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            Object original = Class.forName("net.onlyjugs.fabric.OpenTradeFabricClient")
                    .getConstructor().newInstance();
            original.getClass().getMethod("onInitializeClient").invoke(original);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to initialize VM Trade client", e);
        }
    }
}
