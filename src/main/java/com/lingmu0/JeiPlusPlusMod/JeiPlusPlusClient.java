package com.lingmu0.JeiPlusPlusMod;

import net.fabricmc.api.ClientModInitializer;

public final class JeiPlusPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JeiPlusPlusConfig.register();
    }
}
