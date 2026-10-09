package com.asdflj.ae2thing.api.adapter.pattern;

import net.minecraft.inventory.Container;

import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;

public class THDualInterfacePatternTerminal implements IPatternTerminalAdapter {

    @Override
    public boolean supportFluid() {
        return true;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerWirelessDualInterfaceTerminal.class;
    }
}
