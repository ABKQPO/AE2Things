package com.asdflj.ae2thing.integration.ae2stuff;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.integration.Mods;

import cpw.mods.fml.common.registry.GameRegistry;

public class Ae2StuffIntegration {

    private static WirelessConnectorBackend backend;

    public static WirelessConnectorBackend wirelessConnectorBackend() {
        if (backend == null) {
            backend = createWirelessConnectorBackend();
        }
        return backend;
    }

    public static ItemStack wirelessBlockStack() {
        if (!Mods.AE2_STUFF.isModLoaded()) {
            return null;
        }
        return GameRegistry.findItemStack(Mods.AE2_STUFF.getID(), "Wireless", 1);
    }

    public static void registerClientOverlayRenderer() {
        if (!Mods.AE2_STUFF.isModLoaded()) {
            return;
        }
        try {
            Ae2StuffClientIntegration.registerOverlayRenderer();
        } catch (LinkageError ignored) {

        }
    }

    private static WirelessConnectorBackend createWirelessConnectorBackend() {
        if (!Mods.AE2_STUFF.isModLoaded()) {
            return new NoOpWirelessConnectorBackend();
        }
        try {
            return new Ae2StuffWirelessConnectorBackend();
        } catch (LinkageError ignored) {
            return new NoOpWirelessConnectorBackend();
        }
    }
}
