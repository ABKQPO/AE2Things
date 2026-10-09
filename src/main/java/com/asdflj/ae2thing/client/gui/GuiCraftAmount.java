package com.asdflj.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.adapter.terminal.ICraftingModeHolder;
import com.asdflj.ae2thing.common.parts.PartInfusionPatternTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;
import com.asdflj.ae2thing.network.CPacketCraftRequest;

import appeng.api.config.CraftingMode;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.core.AEConfig;

/**
 * The craft amount screen. It inherits upstream's implementation so the editable amount buttons, the lite crafting mode
 * toggle and the rendered craftable slot all stay in sync with AE2. Only the two AE2Things specific pieces are added on
 * top: the tab back to the terminal this screen was opened from, and the craft request itself, which has to keep
 * travelling through {@link CPacketCraftRequest} so the confirmation screen still opens via {@link GuiType}.
 */
public class GuiCraftAmount extends appeng.client.gui.implementations.GuiCraftAmount {

    private GuiTabButton backToTerminalBtn;
    private GuiType originalGui;
    private ItemStack myIcon;

    public GuiCraftAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountTextField.setMaxStringLength(20);
        // GuiSub adds its own tab as soon as the container exposes a primary GUI icon, but that tab asks the server to
        // open its stored PrimaryGui and AE2Things' containers never store one, so clicking it throws inside
        // PacketSwitchGuis. AE2Things builds the tab itself below and switches through its own GuiType instead.
        if (super.originalGuiBtn != null) {
            this.buttonList.remove(super.originalGuiBtn);
            super.originalGuiBtn = null;
        }
        final Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
        if (target instanceof PartInfusionPatternTerminal) {
            this.myIcon = ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
            this.originalGui = GuiType.INFUSION_PATTERN_TERMINAL;
        } else if (target instanceof WirelessDualInterfaceTerminalInventory) {
            this.myIcon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            this.originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        }
        if (this.originalGui != null && this.myIcon != null) {
            this.buttonList.add(
                this.backToTerminalBtn = new GuiTabButton(
                    this.guiLeft + 151,
                    this.guiTop - 4,
                    this.myIcon,
                    this.myIcon.getDisplayName(),
                    itemRender));
            this.backToTerminalBtn.setHideEdge(13);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == this.backToTerminalBtn) {
            InventoryHandler.switchGui(this.originalGui);
            return;
        }
        if (btn == this.nextBtn && btn.enabled) {
            // Upstream sends AE2's PacketCraftRequest from here. AE2Things keeps its own packet because its handler
            // opens GuiType.CRAFTING_CONFIRM / CRAFTING_CONFIRM_ITEM and wires up the item and the auto start itself.
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketCraftRequest(
                    this.getAmountLong(),
                    isShiftKeyDown(),
                    isCtrlKeyDown(),
                    (CraftingMode) ((ICraftingModeHolder) this).ae2thing$getCraftingMode()
                        .getCurrentValue(),
                    AEConfig.instance.getUseLiteCraftingMode()));
            return;
        }
        super.actionPerformed(btn);
    }
}
