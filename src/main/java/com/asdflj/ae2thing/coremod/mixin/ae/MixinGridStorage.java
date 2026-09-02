package com.asdflj.ae2thing.coremod.mixin.ae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.api.CraftingDebugHelper;

import appeng.me.GridStorage;

@Mixin(value = GridStorage.class, remap = false)
public abstract class MixinGridStorage {

    @Inject(
        method = "remove()V",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/worlddata/IWorldGridStorageData;destroyGridStorage(J)V",
            shift = At.Shift.AFTER),
        remap = false)
    private void ae2thing$remove(CallbackInfo ci) {
        CraftingDebugHelper.remove((GridStorage) (Object) this);
    }
}
