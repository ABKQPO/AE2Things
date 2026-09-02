package com.asdflj.ae2thing.coremod.mixin.ae2fc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.asdflj.ae2thing.coremod.hooker.CoreModHooksClient;
import com.glodblock.github.client.gui.GuiFluidInterface;

import appeng.client.gui.AEBaseGui;

@Mixin(value = GuiFluidInterface.class, remap = false)
public abstract class MixinGuiFluidInterface {

    @Redirect(
        method = "drawFG(IIII)V",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/AEBaseGui;getGuiDisplayName(Ljava/lang/String;)Ljava/lang/String;"),
        remap = false)
    private String ae2thing$translateDisplayName(AEBaseGui gui, String text) {
        return CoreModHooksClient.translateToLocal(text, (GuiFluidInterface) gui);
    }
}
