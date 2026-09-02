package com.asdflj.ae2thing.inventory.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.integration.Mods;

final class OptionalWCTGuiFactory implements IGuiFactory {

    @Override
    public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        IGuiFactory delegate = getDelegate();
        return delegate == null ? null : delegate.createServerGui(player, world, x, y, z, face);
    }

    @Override
    public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        IGuiFactory delegate = getDelegate();
        return delegate == null ? null : delegate.createClientGui(player, world, x, y, z, face);
    }

    private static IGuiFactory getDelegate() {
        return Mods.WIRELESS_CRAFTING_TERMINAL.isModLoaded() ? WCTGuiFactoryHolder.INSTANCE : null;
    }

    private static final class WCTGuiFactoryHolder {

        private static final IGuiFactory INSTANCE = new WCTGuiFactory();
    }
}
