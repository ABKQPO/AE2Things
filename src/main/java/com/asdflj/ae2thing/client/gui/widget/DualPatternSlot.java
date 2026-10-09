package com.asdflj.ae2thing.client.gui.widget;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;

import appeng.api.storage.StorageName;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;

public class DualPatternSlot extends PatternValueSlot {

    public DualPatternSlot(int x, int y, AEStackInventorySyncHandler syncHandler, int slotIndex,
        ContainerWirelessDualInterfaceTerminal container) {
        super(x, y, syncHandler, slotIndex, (slot, type, _) -> {
            if (slot.getStorageName() == StorageName.CRAFTING_INPUT && container.isCraftingMode()) {
                return type == ITEM_STACK_TYPE;
            }
            return true;
        });
    }
}
