package com.asdflj.ae2thing.coremod.mixin.ae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.container.ContainerSubGui;
import appeng.container.PrimaryGui;

@Mixin(ContainerSubGui.class)
public abstract class MixinContainerSubGui {

    @Inject(method = "setPrimaryGui", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2t$skipInvalidPrimaryGui(PrimaryGui primaryGui, CallbackInfo ci) {
        if (primaryGui == null || primaryGui.getIcon() == null) ci.cancel();
    }
}
