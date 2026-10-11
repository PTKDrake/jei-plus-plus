package com.lingmu0.JeiPlusPlusMod.client;

import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

/** Keeps Fabric's exact droplet quantities while exposing mB to the recipe tree. */
final class FabricFluidStack {
    static final FabricFluidStack EMPTY = new FabricFluidStack(FluidVariant.blank(), 0L);
    private final FluidVariant variant;
    private final long droplets;

    FabricFluidStack(Fluid fluid, int millibuckets) {
        this(FluidVariant.of(fluid), (long) Math.max(0, millibuckets) * FluidConstants.BUCKET / 1000L);
    }

    FabricFluidStack(FluidVariant variant, long droplets) {
        this.variant = variant;
        this.droplets = Math.max(0L, droplets);
    }

    static FabricFluidStack from(IJeiFluidIngredient ingredient) {
        return new FabricFluidStack(FluidVariant.of(ingredient.getFluid(), ingredient.getTag().orElse(null)), ingredient.getAmount());
    }

    /** Optional Create/Porting Lib fluid stacks also use Fabric droplet units. */
    static Optional<FabricFluidStack> fromForeign(Object candidate) {
        if (candidate instanceof IJeiFluidIngredient ingredient) return Optional.of(from(ingredient));
        if (candidate == null) return Optional.empty();
        try {
            Object fluid = candidate.getClass().getMethod("getFluid").invoke(candidate);
            Object amount = candidate.getClass().getMethod("getAmount").invoke(candidate);
            if (!(amount instanceof Number number)) return Optional.empty();
            FluidVariant variant = fluid instanceof FluidVariant value ? value
                : fluid instanceof Fluid value ? FluidVariant.of(value) : null;
            if (variant == null) return Optional.empty();
            return Optional.of(new FabricFluidStack(variant, number.longValue()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    Fluid getFluid() { return variant.getFluid(); }
    int getAmount() {
        long unit = FluidConstants.BUCKET / 1000L;
        return (int) Math.min(Integer.MAX_VALUE, droplets / unit + (droplets % unit == 0 ? 0 : 1));
    }
    boolean isEmpty() { return variant.isBlank() || getFluid() == Fluids.EMPTY || droplets <= 0; }
    FabricFluidStack copy() { return this; }
    FabricFluidStack scale(int batches) {
        long amount;
        try {
            amount = Math.multiplyExact(droplets, Math.max(1, batches));
        } catch (ArithmeticException ignored) {
            amount = Long.MAX_VALUE;
        }
        return new FabricFluidStack(variant, amount);
    }

    IJeiFluidIngredient ingredient() {
        return new mezz.jei.fabric.ingredients.fluid.JeiFluidIngredient(variant.getFluid(), droplets, variant.getNbt());
    }

    static boolean isSameFluidSameComponents(FabricFluidStack first, FabricFluidStack second) {
        return first.variant.equals(second.variant);
    }
}
