package com.lingmu0.JeiPlusPlusMod;

import net.fabricmc.api.ModInitializer;

/** Common entry point, safe to load without JEI or any client classes. */
public final class JeiPlusPlus implements ModInitializer {
    public static final String MODID = "jei_plus_plus";

    @Override
    public void onInitialize() {
        Ae2PatternNetwork.register();
    }
}
