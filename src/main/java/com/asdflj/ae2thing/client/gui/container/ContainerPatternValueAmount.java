package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

import appeng.api.config.SecurityPermissions;
import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotInaccessible;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.tile.inventory.AppEngInternalInventory;

public class ContainerPatternValueAmount extends AEBaseContainer {

    private final Slot patternValue;

    public int valueIndex;
    private final IntSyncHandler valueIndexSync;

    public ContainerPatternValueAmount(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
        final SyncRegistrar sync = this.syncRegistrar();
        this.valueIndexSync = sync.intS2C("valueIndex")
            .onClientChange((o, n) -> this.valueIndex = n);
        this.patternValue = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 34, 53);
        this.addSlotToContainer(patternValue);
        this.bindPlayerInventory(ip, -1000, -1000);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    public Slot getPatternValue() {
        return patternValue;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
        this.valueIndexSync.set(valueIndex);
    }

    @Override
    public boolean isValidContainer() {
        return true;
    }
}
