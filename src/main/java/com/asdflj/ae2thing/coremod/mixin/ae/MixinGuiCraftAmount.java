package com.asdflj.ae2thing.coremod.mixin.ae;

import net.minecraft.inventory.Container;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.asdflj.ae2thing.api.adapter.terminal.ICraftingModeHolder;
import com.asdflj.ae2thing.api.adapter.terminal.IGuiCraftAmount;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.slots.VirtualMESlotSingle;
import appeng.client.gui.widgets.GuiImgButton;

@Mixin(GuiCraftAmount.class)
public abstract class MixinGuiCraftAmount extends GuiAmount implements IGuiCraftAmount, ICraftingModeHolder {

    @Shadow(remap = false)
    private GuiImgButton craftingMode;

    @Final
    @Shadow(remap = false)
    private VirtualMESlotSingle slot;

    public MixinGuiCraftAmount(Container container) {
        super(container);
    }

    @Override
    public GuiImgButton ae2thing$getCraftingMode() {
        return this.craftingMode;
    }

    @Override
    public int getAmount() {
        return IGuiCraftAmount.super.getAmount();
    }

    @Override
    public long getAmountLong() {
        return super.getAmountLong();
    }

    @Override
    public void setAmount(long amount) {
        this.amountTextField.setText(String.valueOf(amount));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }

    @Override
    public @Nullable IAEStack<?> getTarget() {
        return this.slot == null ? null : this.slot.getAEStack();
    }
}
