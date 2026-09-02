package com.asdflj.ae2thing.coremod.mixin.ae;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.api.CraftingDebugHelper;

import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEStack;
import appeng.crafting.v2.CraftingJobV2;

@Mixin(value = CraftingJobV2.class, remap = false)
public abstract class MixinCraftingJobV2 {

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lappeng/api/networking/IGrid;Lappeng/api/networking/security/BaseActionSource;Lappeng/api/storage/data/IAEStack;Lappeng/api/config/CraftingMode;Lappeng/api/networking/crafting/ICraftingCallback;)V",
        at = @At("TAIL"),
        remap = false)
    private void ae2thing$init(World world, IGrid grid, BaseActionSource actionSource, IAEStack<?> what,
        CraftingMode craftingMode, ICraftingCallback callback, CallbackInfo ci) {
        CraftingDebugHelper
            .craftingHelper((CraftingJobV2) (Object) this, world, grid, actionSource, what, craftingMode, callback);
    }
}
