package com.lingmu0.JeiPlusPlusMod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla container geometry, without Forge/NeoForge's added getters. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int jeiPlusPlus$getLeftPos();
    @Accessor("topPos") int jeiPlusPlus$getTopPos();
}
