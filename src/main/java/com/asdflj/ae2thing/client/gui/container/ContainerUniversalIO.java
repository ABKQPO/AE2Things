package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.helpers.IPrimaryGuiIconProvider;
import appeng.parts.automation.PartSharedItemBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class ContainerUniversalIO extends ContainerUpgradeable implements IVirtualSlotHolder {

    private final PartSharedItemBus<?> bus;
    private final IAEStack<?>[] configClientSlot = new IAEStack[9];

    public ContainerUniversalIO(InventoryPlayer ip, PartSharedItemBus<?> te) {
        super(ip, te);
        this.bus = te;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (Platform.isServer()) {
            this.updateVirtualSlots(StorageName.CONFIG, this.getConfig(), this.configClientSlot);
        }
    }

    @Override
    public void receiveSlotStacks(StorageName invName, Int2ObjectMap<IAEStack<?>> slotStacks) {
        final IAEStackInventory config = this.getConfig();
        for (Int2ObjectMap.Entry<IAEStack<?>> entry : slotStacks.int2ObjectEntrySet()) {
            config.putAEStackInSlot(entry.getIntKey(), entry.getValue());
        }
        if (Platform.isServer()) {
            this.updateVirtualSlots(StorageName.CONFIG, config, this.configClientSlot);
        }
    }

    public IAEStackInventory getConfig() {
        return this.bus.getAEInventoryByName(StorageName.CONFIG);
    }

    public PartSharedItemBus<?> getBus() {
        return this.bus;
    }

    @Override
    public PrimaryGui createPrimaryGui() {
        final ContainerOpenContext ctx = this.getOpenContext();
        if (ctx == null || ctx.getTile() == null || ctx.getSide() == null) return super.createPrimaryGui();
        return AE2ThingPrimaryGui.create(
            GuiType.UNIVERSAL_BUS,
            this.bus instanceof IPrimaryGuiIconProvider p ? p.getPrimaryGuiIcon() : null,
            new BlockPos(ctx.getTile()),
            ctx.getSide());
    }
}
