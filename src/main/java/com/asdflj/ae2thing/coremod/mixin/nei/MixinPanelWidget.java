package com.asdflj.ae2thing.coremod.mixin.nei;

import static com.asdflj.ae2thing.nei.NEI_TH_Config.getConfigValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.adapter.terminal.IGuiCraftAmount;
import com.asdflj.ae2thing.api.adapter.terminal.ITerminal;
import com.asdflj.ae2thing.client.event.UpdateAmountTextEvent;
import com.asdflj.ae2thing.nei.ButtonConstants;
import com.asdflj.ae2thing.network.CPacketCraftRequest;
import com.asdflj.ae2thing.network.CPacketInventoryAction;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.Util;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.helpers.InventoryAction;
import appeng.helpers.MonitorableAction;
import appeng.util.Platform;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PanelWidget;
import codechicken.nei.Widget;
import codechicken.nei.guihook.IContainerTooltipHandler;

@Mixin(PanelWidget.class)
public abstract class MixinPanelWidget extends Widget implements IContainerTooltipHandler {

    @Shadow(remap = false)
    public abstract ItemStack getStackMouseOver(int mousex, int mousey);

    @Shadow(remap = false)
    public ItemStack draggedStack;

    @Inject(method = "handleClick", at = @At(value = "HEAD"), remap = false, cancellable = true)
    public void handleClick(int mousex, int mousey, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 2) return;
        try {
            ItemStack is = this.getStackMouseOver(mousex, mousey);
            if (is == null) return;
            IAEStack<?> aes = Util.toAEStack(is);
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui instanceof IGuiCraftAmount g) {
                final long amount = Math.clamp(aes.getStackSize(), 1, Long.MAX_VALUE);
                final IAEStack<?> current = g.getTarget();
                if (Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerCraftAmount
                    && (current == null || !current.isSameType(aes))) {
                    aes.setStackSize(amount);
                    AE2Thing.proxy.netHandler.sendToServer(new CPacketCraftRequest(aes, amount));
                }
                g.setAmount(amount);
                draggedStack = null;
                cir.setReturnValue(true);
            } else if (gui instanceof AEBaseGui g) {
                IDisplayRepo repo = Util.getDisplayRepo(g);
                if (repo == null) return;
                if (NEIClientUtils.altKey()) {
                    repo.setSearchString(is.getDisplayName());
                    Util.setSearchFieldText(g, Platform.getItemDisplayName(is));
                } else if (g.inventorySlots instanceof AEBaseContainer c) {
                    MonitorableAction action = null;
                    if (NEIClientUtils.shiftKey()) {
                        action = MonitorableAction.PICKUP_OR_SET_DOWN;
                    } else if (GuiScreen.isCtrlKeyDown()) {
                        action = MonitorableAction.PICKUP_SINGLE;
                    } else if (getConfigValue(ButtonConstants.NEI_CRAFT_ITEM) && repo instanceof ItemRepo itemRepo
                        && canCraft(itemRepo, aes)) {
                            if (aes.getStackSize() <= 0) {
                                aes.setStackSize(1);
                            }
                            if (c.getTarget() instanceof IGuiItemObject o && o.getItemStack() != null
                                && o.getItemStack()
                                    .getItem() != null) {
                                openCraftAmount(
                                    o.getItemStack()
                                        .getItem()
                                        .getClass(),
                                    c,
                                    aes);
                            } else if (c.getTarget() instanceof IPart p) {
                                ItemStack part = p.getItemStack(PartItemStack.Network);
                                if (part != null && part.getItem() != null) {
                                    openCraftAmount(
                                        part.getItem()
                                            .getClass(),
                                        c,
                                        aes);
                                }
                            }
                        }
                    if (action != null) {
                        c.setTargetStack(aes);
                        final PacketMonitorableAction p = new PacketMonitorableAction(action, -1);
                        NetworkHandler.instance.sendToServer(p);
                    }
                }
                draggedStack = null;
                cir.setReturnValue(true);
            }
        } catch (Exception ignored) {}
    }

    private boolean canCraft(ItemRepo repo, IAEStack<?> aes) {
        if (repo == null || aes == null) return false;

        final IItemList<IAEStack<?>> list = Ae2ReflectClient.getList(repo);
        if (list == null) return false;
        final IAEStack<?> found = list.findPrecise(aes);
        return found != null && found.isCraftable();
    }

    private void openCraftAmount(Class<? extends Item> o, AEBaseContainer c, IAEStack<?> aes) {
        for (ITerminal terminal : AE2ThingAPI.instance()
            .terminal()
            .getTerminalSet()) {
            if (terminal.getClasses()
                .contains(o)) {
                c.setTargetStack(aes);
                terminal.openCraftAmount();
                MinecraftForge.EVENT_BUS.post(new UpdateAmountTextEvent(aes.getStackSize()));
                return;
            }
        }

        c.setTargetStack(aes);
        AE2Thing.proxy.netHandler.sendToServer(new CPacketInventoryAction(InventoryAction.AUTO_CRAFT, 0, 0, aes));
    }
}
