package com.asdflj.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;

import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;

public class GuiCraftConfirm extends appeng.client.gui.implementations.GuiCraftConfirm {

    public GuiCraftConfirm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        final Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
        GuiType originalGui = null;
        if (target instanceof PartInfusionPatternTerminal) {
            originalGui = GuiType.INFUSION_PATTERN_TERMINAL;
        } else if (target instanceof WirelessDualInterfaceTerminalInventory) {
            originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        }
        if (btn == this.getCancelButton() && originalGui != null
            && ((AEBaseContainer) this.inventorySlots).getPrimaryGui() == null) {
            // only acts when shift is held and items are missing, exactly like upstream's handler
            this.addMissingItemsToBookMark();
            InventoryHandler.switchGui(originalGui);
            return;
        }
        super.actionPerformed(btn);
    }
}
