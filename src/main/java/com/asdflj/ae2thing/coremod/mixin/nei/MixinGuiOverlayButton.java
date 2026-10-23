package com.asdflj.ae2thing.coremod.mixin.nei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.common.MinecraftForge;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.client.event.GuiOverlayButtonEvent;
import com.asdflj.ae2thing.nei.AEItemOverlayState;
import com.asdflj.ae2thing.nei.OverlayStacks;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

@Mixin(GuiOverlayButton.class)
public abstract class MixinGuiOverlayButton extends GuiRecipeButton {

    @Final
    @Shadow
    public GuiContainer firstGui;

    protected MixinGuiOverlayButton(RecipeHandlerRef point, int x, int y, int buttonId, String label) {
        super(point, x, y, buttonId, label);
    }

    @Inject(method = "mouseReleased", at = @At(value = "TAIL"))
    public void mouseReleased(int mouseX, int mouseY, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new GuiOverlayButtonEvent((GuiOverlayButton) ((Object) this)));
    }

    @Inject(method = "drawItemOverlay", at = @At(value = "TAIL"), remap = false)
    public void ae2thing$drawOutputOverlay(CallbackInfo ci) {
        final int presenceOverlay = NEIClientConfig.itemPresenceOverlay();
        if (presenceOverlay <= 0) return;
        final IItemList<IAEStack<?>> list = OverlayStacks.listFor(this.firstGui);
        if (list == null) return;
        final IRecipeHandler handler = this.handlerRef.handler;
        final int recipeIndex = this.handlerRef.recipeIndex;
        final List<PositionedStack> stacks = new ArrayList<>();
        try {
            final PositionedStack result = handler.getResultStack(recipeIndex);
            if (result != null) stacks.add(result);
            final List<PositionedStack> others = handler.getOtherStacks(recipeIndex);
            if (others != null) stacks.addAll(others);
        } catch (Throwable ignored) {
            return;
        }
        final GuiOverlayButton.ItemOverlayFormat format = GuiOverlayButton.ItemOverlayFormat.from(presenceOverlay);
        final Set<Long> drawn = new HashSet<>();
        for (PositionedStack stack : stacks) {
            if (stack == null || stack.item == null) continue;
            if (!drawn.add((((long) stack.relx) << 32) ^ (stack.rely & 0xFFFFFFFFL))) continue;
            final OverlayStacks.Presence presence = OverlayStacks.presence(list, stack);
            new GuiOverlayButton.ItemOverlayState(stack, presence.stored()).draw(format);
            if (presence.craftable()) AEItemOverlayState.drawPatternIcon(stack);
        }
    }
}
