package com.asdflj.ae2thing.client.gui.container;

import java.util.Objects;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.common.parts.THPart;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.api.AEApi;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.util.Platform;

public class ContainerCraftConfirm extends appeng.container.implementations.ContainerCraftConfirm {

    public ContainerCraftConfirm(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void setOpenContext(final ContainerOpenContext context) {
        super.setOpenContext(context);
        if (Platform.isServer() && this.getPrimaryGui() == null) {
            setPrimaryGui(createPrimaryGui());
        }
    }

    @Override
    public PrimaryGui createPrimaryGui() {
        final GuiType originalGui = originalGuiType();
        if (originalGui == null) return super.createPrimaryGui();
        return ContainerPatternValueAmount.primaryGui(originalGui, originIcon(originalGui), originPos(), originSide());
    }

    @Nullable
    private GuiType originalGuiType() {
        final IActionHost ah = this.getActionHost();
        if (ah instanceof PartInfusionPatternTerminal) return GuiType.INFUSION_PATTERN_TERMINAL;
        if (ah instanceof WirelessDualInterfaceTerminalInventory) return GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        return null;
    }

    private ItemStack originIcon(final GuiType type) {
        final ItemStack icon = switch (type) {
            case INFUSION_PATTERN_TERMINAL -> ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
            case WIRELESS_DUAL_INTERFACE_TERMINAL -> ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            default -> null;
        };
        if (icon != null) return icon;
        return AEApi.instance()
            .definitions()
            .parts()
            .terminal()
            .maybeStack(1)
            .orNull();
    }

    private BlockPos originPos() {
        final ContainerOpenContext ctx = this.getOpenContext();
        if (ctx != null && ctx.getTile() != null) return new BlockPos(ctx.getTile());
        final IActionHost ah = this.getActionHost();
        if (ah instanceof WirelessTerminal wt) return new BlockPos(wt.getInventorySlot(), 0, 0);
        return new BlockPos(0, 0, 0);
    }

    private ForgeDirection originSide() {
        final ContainerOpenContext ctx = this.getOpenContext();
        return ctx != null && ctx.getSide() != null ? ctx.getSide() : ForgeDirection.UNKNOWN;
    }

    @Override
    public void switchToOriginalGUI() {
        GuiType originalGui = null;

        final IActionHost ah = this.getActionHost();
        if (ah instanceof PartInfusionPatternTerminal) {
            originalGui = GuiType.INFUSION_PATTERN_TERMINAL;
        } else if (ah instanceof WirelessDualInterfaceTerminalInventory) {
            originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        }

        if (this.getOpenContext() != null && ah instanceof THPart) {
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(
                    this.getOpenContext()
                        .getTile()),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        } else if (ah instanceof WirelessTerminal) {
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(((WirelessTerminal) ah).getInventorySlot(), 0, 0),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        }
    }
}
