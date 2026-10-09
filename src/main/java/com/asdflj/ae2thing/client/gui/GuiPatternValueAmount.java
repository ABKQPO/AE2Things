package com.asdflj.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.ae2thing.api.adapter.terminal.IGuiCraftAmount;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternValueAmount;

import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMESlotSingle;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;

public class GuiPatternValueAmount extends appeng.client.gui.implementations.GuiAmount implements IGuiCraftAmount {

    private final ContainerPatternValueAmount container;
    private final VirtualMESlotSingle slot;

    public GuiPatternValueAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerPatternValueAmount(inventoryPlayer, te));
        this.container = (ContainerPatternValueAmount) this.inventorySlots;
        this.slot = new VirtualMESlotSingle(34, 53, 0, null);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountTextField.setMaxStringLength(20);
        this.registerVirtualSlots(this.slot);
        this.update();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.nextBtn.displayString = GuiText.Set.getLocal();
        try {
            this.nextBtn.enabled = this.getAmountLong() > 0;
        } catch (final NumberFormatException e) {
            this.nextBtn.enabled = false;
        }
        this.amountTextField.drawTextBox();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == this.nextBtn && btn.enabled
            && this.container.getAEStack() != null
            && this.container.getInvName() != null) {
            final IAEStack<?> aes = this.container.getAEStack()
                .copy();
            aes.setStackSize(this.getAmountLong());
            NetworkHandler.instance.sendToServer(
                new PacketPatternValueSet(aes, this.container.getInvName(), this.container.getSlotIndex()));
            return;
        }
        super.actionPerformed(btn);
    }

    @Override
    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    public void update() {
        final IAEStack<?> aes = this.container.getAEStack();
        this.slot.setAEStack(aes);
        if (aes != null) {
            this.setAmount(aes.getStackSize());
        }
    }

    @Override
    public int getAmount() {
        try {
            return (int) Math.min(Integer.MAX_VALUE, this.getAmountLong());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public long getAmountLong() {
        final String text = this.amountTextField.getText()
            .trim();
        try {
            return Long.parseLong(text);
        } catch (final NumberFormatException e) {
            return super.getAmountLong();
        }
    }

    @Override
    public void setAmount(int amount) {
        this.setAmount((long) amount);
    }

    public void setAmount(long amount) {
        this.amountTextField.setText(Long.toString(amount));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }
}
