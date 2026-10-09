package com.asdflj.ae2thing.client.gui.widget;

import static appeng.client.gui.implementations.GuiMEMonitorable.keyBindPickBlockAction;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.InventoryActionExtend;
import com.asdflj.ae2thing.network.CPacketInventoryActionExtend;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMEPatternSlot;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;

public class PatternValueSlot extends VirtualMEPatternSlot {

    private final boolean quantityless;

    public PatternValueSlot(int x, int y, AEStackInventorySyncHandler syncHandler, int slotIndex,
        TypeAcceptPredicate acceptType) {
        this(x, y, syncHandler, slotIndex, acceptType, false);
    }

    public PatternValueSlot(int x, int y, AEStackInventorySyncHandler syncHandler, int slotIndex,
        TypeAcceptPredicate acceptType, boolean quantityless) {
        super(x, y, syncHandler, slotIndex, acceptType);
        this.quantityless = quantityless;
    }

    @Override
    public void handleMouseClicked(@Nullable ItemStack itemStack, boolean isExtraAction, int mouseButton) {
        if (mouseButton == keyBindPickBlockAction) {
            if (this.quantityless) return;
            final IAEStack<?> stack = this.getAEStack();
            if (stack != null) {
                final boolean rename = isExtraAction || GuiScreen.isCtrlKeyDown();
                AE2Thing.proxy.netHandler.sendToServer(
                    new CPacketInventoryActionExtend(
                        rename ? InventoryActionExtend.SET_PATTERN_NAME : InventoryActionExtend.SET_PATTERN_VALUE,
                        this.getSlotIndex(),
                        this.getStorageName()
                            .ordinal(),
                        stack.copy()));
                return;
            }
        }
        super.handleMouseClicked(itemStack, isExtraAction, mouseButton);
    }
}
