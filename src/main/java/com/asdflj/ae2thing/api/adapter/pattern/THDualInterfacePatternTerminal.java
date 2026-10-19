package com.asdflj.ae2thing.api.adapter.pattern;

import java.util.List;

import net.minecraft.inventory.Container;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.NEIUtils;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.asdflj.ae2thing.network.CPacketTransferRecipe;
import com.science.gtnl.config.MainConfig;

public class THDualInterfacePatternTerminal implements IPatternTerminalAdapter {

    @Override
    public boolean supportFluid() {
        return true;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerWirelessDualInterfaceTerminal.class;
    }

    @Override
    public void transfer(Container container, List<OrderStack<?>> inputs, List<OrderStack<?>> outputs,
        String identifier, CPacketTransferRecipe message) {
        if (Mods.SNL.isModLoaded() && MainConfig.other.not_enough_items.enableReplaceHotIngotInPattern
            && !Constants.NEI_MOUSE_WHEEL.equals(identifier)
            && message != null
            && !message.isCraft) {
            outputs = NEIUtils.replaceHotIngot(outputs);
        }

        IPatternTerminalAdapter.super.transfer(container, inputs, outputs, identifier, message);
    }
}
