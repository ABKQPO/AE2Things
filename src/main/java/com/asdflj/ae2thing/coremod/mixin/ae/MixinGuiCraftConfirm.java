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

    /**
     * Set when a replan was requested. The visible plan is only dropped once the replanned data arrives, so a refused
     * request cannot leave the player with an empty screen.
     */
    @Unique
    private boolean ae2thing$awaitingReplan = false;

    /** The crafting tree is requested once per screen (and again after a replan) when the tree view is opened. */
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
            // The old plan is kept until the replanned data arrives: the request can be refused (see Util#replan) and
            // dropping the lists here would leave the player with an empty screen. Rebuilding them on the first
            // incoming update still avoids stale entries, whose usedPercent would otherwise be dropped by
            // handleInput().
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

        // AE2 ships the crafting tree together with the plan, and only for the full (non-lite) crafting job. Ask for it
        // when the tree view is opened so it is available even if the original packet was missed, and after a replan.
        if (!this.ae2thing$treeRequested && this.displayMode == GuiCraftConfirm.DisplayMode.TREE) {
            this.ae2thing$treeRequested = true;
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("GuiCraftConfirm.requestTree", true));
        }
    }
}
