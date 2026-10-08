package com.asdflj.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiTabButton;

public class GuiCraftingStatus extends appeng.client.gui.implementations.GuiCraftingStatus {

    private final ITerminalHost host;
    private GuiType originalGui;
    private ItemStack originalGuiIcon;

    public GuiCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        host = te;
    }

    @Override
    public void initGui() {
        if (host instanceof PartInfusionPatternTerminal) {
            originalGuiIcon = ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
            originalGui = GuiType.INFUSION_PATTERN_TERMINAL;
        } else if (host instanceof WirelessDualInterfaceTerminalInventory) {
            originalGuiIcon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        }
        super.initGui();

        if (this.originalGui != null && this.originalGuiIcon != null
            && !this.buttonList.contains(this.originalGuiBtn)) {
            this.buttonList.add(
                this.originalGuiBtn = new GuiTabButton(
                    this.guiLeft + this.xSize - 25,
                    this.guiTop - 4,
                    this.originalGuiIcon,
                    this.originalGuiIcon.getDisplayName(),
                    itemRender));
            this.originalGuiBtn.setHideEdge(13);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == this.originalGuiBtn && this.originalGui != null) {
            InventoryHandler.switchGui(this.originalGui);
            return;
        }
        super.actionPerformed(btn);
    }
}
