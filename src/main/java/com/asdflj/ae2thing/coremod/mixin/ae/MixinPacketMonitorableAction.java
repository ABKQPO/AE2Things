package com.asdflj.ae2thing.coremod.mixin.ae;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.asdflj.ae2thing.client.gui.container.ContainerMonitor;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.helpers.MonitorableAction;

@Mixin(PacketMonitorableAction.class)
public abstract class MixinPacketMonitorableAction {

    @Shadow(remap = false)
    @Final
    private MonitorableAction action;

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void handleAE2ThingContainer(INetworkInfo manager, AppEngPacket packet, EntityPlayer player,
        CallbackInfo ci) {
        if (player.openContainer instanceof ContainerMonitor container) {
            container.doMonitorableAction(this.action, (EntityPlayerMP) player);
            ci.cancel();
        }
    }
}
