package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;

import com.asdflj.ae2thing.util.Util;

import appeng.api.storage.ITerminalHost;
import appeng.container.AEBaseContainer;

public class ContainerRenamer extends AEBaseContainer {

    private Util.DimensionalCoordSide renamingTarget;

    public ContainerRenamer(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
    }

    public Util.DimensionalCoordSide getRenamingTarget() {
        return this.renamingTarget;
    }

    public void setRenamingTarget(Util.DimensionalCoordSide renamingTarget) {
        this.renamingTarget = renamingTarget;
    }
}
