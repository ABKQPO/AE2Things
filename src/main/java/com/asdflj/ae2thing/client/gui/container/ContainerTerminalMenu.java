package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerTerminalMenu extends Container {

    public ContainerTerminalMenu(InventoryPlayer inventory) {
        super();
        this.addPlayerInventory(inventory);
    }

    private void addPlayerInventory(InventoryPlayer inventory) {
        for (int slot = 0; slot < inventory.mainInventory.length; slot++) {
            this.addSlotToContainer(new Slot(inventory, slot, -1000, -1000));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
