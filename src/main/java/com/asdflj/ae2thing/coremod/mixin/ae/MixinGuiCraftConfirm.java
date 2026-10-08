package com.asdflj.ae2thing.coremod.mixin.ae;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.client.gui.widgets.GuiAeButton;

@Mixin(GuiCraftConfirm.class)
public abstract class MixinGuiCraftConfirm extends AEBaseGui {

    @Shadow(remap = false)
    private GuiAeButton start;

    @Shadow(remap = false)
    private GuiAeButton startWithFollow;

    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> storage;
    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> pending;
    @Shadow(remap = false)
    @Final
    @Mutable
    private IItemList<IAEStack<?>> missing;
    @Shadow(remap = false)
    @Final
    private List<IAEStack<?>> visual;
    @Shadow(remap = false)
    @Final
    private List<IAEStack<?>> filteredVisual;

    @Shadow(remap = false)
    private GuiCraftConfirm.DisplayMode displayMode;

    private GuiAeButton replan = null;
    private boolean clickStart = false;

    @Unique
    private boolean ae2thing$awaitingReplan = false;

    @Unique
    private boolean ae2thing$treeRequested = false;

    public MixinGuiCraftConfirm(Container container) {
        super(container);
    }

    @Inject(method = "actionPerformed", at = @At(value = "HEAD"))
    private void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (btn == start || btn == startWithFollow) {
            clickStart = true;
        } else if (btn == replan) {
            clickStart = false;
            start.enabled = false;
            replan.visible = false;
            this.ae2thing$awaitingReplan = true;
            this.ae2thing$treeRequested = false;
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.replan", true));
        }
    }

    @Inject(method = "postUpdate", at = @At("HEAD"), remap = false)
    public void postUpdate(List<IAEStack<?>> list, byte ref, CallbackInfo ci) {
        if (!this.ae2thing$awaitingReplan) {
            return;
        }
        this.ae2thing$awaitingReplan = false;
        this.storage = AEApi.instance()
            .storage()
            .createAEStackList();
        this.pending = AEApi.instance()
            .storage()
            .createAEStackList();
        this.missing = AEApi.instance()
            .storage()
            .createAEStackList();
        this.visual.clear();
        this.filteredVisual.clear();
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(
            replan = new GuiAeButton(
                0,
                start.xPosition,
                start.yPosition,
                start.width,
                start.height,
                I18n.format(NameConst.GUI_BUTTON_REPLAN),
                ""));
        this.replan.visible = false;
    }

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false)
    public void drawFG(CallbackInfo ci) {
        try {
            if (clickStart || !start.enabled) {
                replan.visible = true;
                start.visible = false;
                startWithFollow.visible = false;
            } else {
                replan.visible = false;
                start.visible = true;
                startWithFollow.visible = true;
            }
        } catch (Exception ignored) {}
        if (!this.ae2thing$treeRequested && this.displayMode == GuiCraftConfirm.DisplayMode.TREE) {
            this.ae2thing$treeRequested = true;
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.requestTree", true));
        }
    }
}
