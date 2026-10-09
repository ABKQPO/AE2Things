package com.asdflj.ae2thing.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.asdflj.ae2thing.client.gui.container.ContainerPatternItemRenamer;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternValueAmount;

import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.GuiSub;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;
import appeng.util.item.AEItemStack;

public class GuiPatternItemRenamer extends GuiSub implements IDropToFillTextField {

    private final ContainerPatternValueAmount container;
    private final MEGuiTextField textField;

    public GuiPatternItemRenamer(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerPatternItemRenamer(inventoryPlayer, te));
        this.container = (ContainerPatternValueAmount) this.inventorySlots;
        this.xSize = 256;
        this.textField = new MEGuiTextField(231, 12);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.textField.x = this.guiLeft + 12;
        this.textField.y = this.guiTop + 35;
        this.textField.setFocused(true);
        this.update();
    }

    public void update() {
        final IAEStack<?> aes = this.container.getAEStack();
        if (aes != null) {
            this.textField.setText(aes.getDisplayName());
            this.textField.setCursorPositionEnd();
            this.textField.setSelectionPos(0);
        }
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.Renamer.getLocal(), 12, 8, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture("guis/renamer.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.textField.drawTextBox();
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        this.textField.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            final IAEStack<?> nameStack = this.getNewNameStack();
            if (nameStack == null) return;
            NetworkHandler.instance.sendToServer(
                new PacketPatternValueSet(nameStack, this.container.getInvName(), this.container.getSlotIndex()));
        } else if (!this.textField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    private IAEStack<?> getNewNameStack() {
        final IAEStack<?> aeStack = this.container.getAEStack();
        if (!(aeStack instanceof IAEItemStack itemStack)) return null;
        return AEItemStack.create(
            itemStack.getItemStack()
                .setStackDisplayName(this.textField.getText()));
    }

    @Override
    public boolean isOverTextField(final int mousex, final int mousey) {
        return this.textField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(final String displayName, final int mousex, final int mousey, final ItemStack stack) {
        this.textField.setText(displayName);
    }
}
