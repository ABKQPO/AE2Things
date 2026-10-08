package com.asdflj.ae2thing.client.gui.container.widget;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.api.Constants;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.helpers.PatternEncodingHelper;
import appeng.items.contents.PinsHandler;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.ConfigManager;
import appeng.util.item.AEItemStack;

/**
 * Adapts AE2Things' slot based pattern terminal ({@link com.asdflj.ae2thing.inventory.IPatternTerminal} backed by
 * {@code AppEngInternalInventory} fake slots) to AE2's {@link appeng.api.parts.IPatternTerminal}, so that writing a
 * pattern is done by AE2's own {@link PatternEncodingHelper} instead of a second, diverging implementation living in
 * this mod.
 * <p>
 * The encoder only ever uses a small part of the interface: the AE stack views of the input/output inventories, the
 * {@code "pattern"} inventory, the crafting/substitute flags and the encode listeners. Everything else is implemented
 * as a harmless stub, because an instance of this bridge is created per encode call and never escapes into AE2's
 * machinery.
 */
public class PatternEncodingBridge implements appeng.api.parts.IPatternTerminal {

    private final com.asdflj.ae2thing.inventory.IPatternTerminal terminal;
    private final SlotBackedAEStackInventory inputs;
    private final SlotBackedAEStackInventory outputs;
    private final IInventory patternInv;
    private final IInventory viewCells = new AppEngInternalInventory(null, 5);
    private final IConfigManager configManager = new ConfigManager(this);
    private final Set<PatternEncodeListener> listeners = Collections.newSetFromMap(new WeakHashMap<>());
    private final IMEMonitor<IAEItemStack> itemMonitor;
    private final boolean craftingMode;
    private final boolean substitution;
    private final boolean beSubstitution;

    private PatternEncodingBridge(final com.asdflj.ae2thing.inventory.IPatternTerminal terminal,
        final boolean craftingMode, final boolean substitution, final boolean beSubstitution,
        final IMEMonitor<IAEItemStack> itemMonitor) {
        this.terminal = terminal;
        this.craftingMode = craftingMode;
        this.substitution = substitution;
        this.beSubstitution = beSubstitution;
        this.itemMonitor = itemMonitor;
        this.patternInv = terminal.getInventoryByName(Constants.PATTERN);
        if (craftingMode) {
            // A crafting pattern is rebuilt from the 3x3 grid by PatternEncodingHelper#getCraftingOutput, no outputs
            // are read from the terminal in that case.
            this.inputs = new SlotBackedAEStackInventory(
                terminal.getInventoryByName(Constants.CRAFTING),
                StorageName.CRAFTING_INPUT);
            this.outputs = new SlotBackedAEStackInventory(null, StorageName.CRAFTING_OUTPUT);
        } else {
            this.inputs = new SlotBackedAEStackInventory(
                terminal.getInventoryByName(Constants.CRAFTING_EX),
                StorageName.CRAFTING_INPUT);
            this.outputs = new SlotBackedAEStackInventory(
                terminal.getInventoryByName(Constants.OUTPUT_EX),
                StorageName.CRAFTING_OUTPUT);
        }
    }

    /**
     * Writes a pattern for the given terminal using AE2's {@link PatternEncodingHelper}.
     *
     * @return whether a pattern was written to the terminal's pattern output slot.
     */
    public static boolean encode(final com.asdflj.ae2thing.inventory.IPatternTerminal terminal,
        final boolean craftingMode, final boolean substitution, final boolean beSubstitution,
        final IMEMonitor<IAEItemStack> itemMonitor, final IEnergySource powerSource,
        final BaseActionSource actionSource, final String author, final World world) {
        if (terminal == null || world == null) {
            return false;
        }
        final PatternEncodingBridge bridge = new PatternEncodingBridge(
            terminal,
            craftingMode,
            substitution,
            beSubstitution,
            itemMonitor);
        if (bridge.patternInv == null) {
            return false;
        }
        return PatternEncodingHelper.encode(bridge, powerSource, itemMonitor, actionSource, author, world);
    }

    @Override
    public boolean encode(final IEnergySource powerSource, final IMEMonitor<IAEItemStack> itemMonitor,
        final BaseActionSource actionSource, final String auther, final World world) {
        return PatternEncodingHelper.encode(this, powerSource, itemMonitor, actionSource, auther, world);
    }

    @Override
    public boolean isCraftingRecipe() {
        return this.craftingMode;
    }

    @Override
    public void setCraftingRecipe(final boolean craftingMode) {
        // the bridge is a snapshot of the terminal state, AE2's encoder never flips the mode
    }

    @Override
    public boolean isSubstitution() {
        return this.substitution;
    }

    @Override
    public void setSubstitution(final boolean canSubstitute) {
        // read only snapshot
    }

    @Override
    public boolean canBeSubstitution() {
        return this.beSubstitution;
    }

    @Override
    public void setCanBeSubstitution(final boolean beSubstitute) {
        // read only snapshot
    }

    @Override
    public IInventory getInventoryByName(final String name) {
        return this.terminal.getInventoryByName(name);
    }

    @Override
    public IAEStackInventory getAEInventoryByName(final StorageName name) {
        switch (name) {
            case CRAFTING_INPUT -> {
                return this.inputs;
            }
            case CRAFTING_OUTPUT -> {
                return this.outputs;
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    public void exPatternTerminalCall(final IAEStack<?>[] in, final IAEStack<?>[] out) {
        // AE2 uses this to push the encoded contents into "Ex" pattern terminals, AE2Things has no such listener
    }

    @Override
    public Iterable<PatternEncodeListener> getPatternEncodeListeners() {
        return this.listeners;
    }

    @Override
    public void addPatternEncodeListeners(final PatternEncodeListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removePatternEncodeListeners(final PatternEncodeListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void saveAEStackInv() {
        // the AE views are backed by the terminal's own inventories, which persist themselves
    }

    @Override
    public void saveChanges() {
        // nothing is written back to the terminal by the encoder
    }

    @Override
    public void onChangeInventory(final IInventory inv, final int slot, final InvOperation mc,
        final ItemStack removedStack, final ItemStack newStack) {
        // nothing is written back to the terminal by the encoder
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
        // the encoder never changes settings
    }

    @Override
    public IInventory getViewCellStorage() {
        return this.viewCells;
    }

    @Override
    public PinsHandler getPinsHandler(final EntityPlayer player) {
        // never queried by the encoder, the bridge does not outlive the encode call
        return null;
    }

    @Override
    public IGrid getGrid() {
        final IGridNode node = this.getGridNode(ForgeDirection.UNKNOWN);
        return node == null ? null : node.getGrid();
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return this.itemMonitor;
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        // the encoder only extracts blank patterns from the item inventory
        return null;
    }

    @Override
    public IMEMonitor<?> getMEMonitor(final @Nonnull IAEStackType<?> type) {
        return this.itemMonitor;
    }

    @Override
    public IGridNode getGridNode(final ForgeDirection dir) {
        if (this.terminal instanceof IGridHost host) {
            return host.getGridNode(dir);
        }
        return null;
    }

    @Override
    public AECableType getCableConnectionType(final ForgeDirection dir) {
        if (this.terminal instanceof IGridHost host) {
            return host.getCableConnectionType(dir);
        }
        return AECableType.NONE;
    }

    @Override
    public void securityBreak() {
        if (this.terminal instanceof IGridHost host) {
            host.securityBreak();
        }
    }

    @Override
    public IGridNode getActionableNode() {
        if (this.terminal instanceof IActionHost host) {
            return host.getActionableNode();
        }
        return this.getGridNode(ForgeDirection.UNKNOWN);
    }

    /**
     * Read only {@link IAEStackInventory} view over a terminal inventory of item stacks, translating AE2FC fluid
     * packets into native AE fluid stacks so AE2 writes the same payload its own pattern terminals do.
     */
    private static final class SlotBackedAEStackInventory extends IAEStackInventory {

        private final IInventory inv;

        private SlotBackedAEStackInventory(final IInventory inv, final StorageName name) {
            super(null, inv == null ? 0 : inv.getSizeInventory(), name);
            this.inv = inv;
        }

        @Override
        public int getSizeInventory() {
            return this.inv == null ? 0 : this.inv.getSizeInventory();
        }

        @Override
        public boolean isEmpty() {
            if (this.inv == null) {
                return true;
            }
            for (int i = 0; i < this.inv.getSizeInventory(); i++) {
                if (this.getAEStackInSlot(i) != null) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public IAEStack<?> getAEStackInSlot(final int n) {
            if (this.inv == null || n < 0 || n >= this.inv.getSizeInventory()) {
                return null;
            }
            final ItemStack stack = this.inv.getStackInSlot(n);
            if (stack == null) {
                return null;
            }
            final IAEFluidStack fluid = ItemFluidPacket.getFluidAEStack(stack);
            if (fluid != null) {
                return fluid;
            }
            return AEItemStack.create(stack);
        }

        @Override
        public void putAEStackInSlot(final int n, final IAEStack<?> aes) {
            // read only view: AE2's encoder never writes into the input/output inventories
        }
    }
}
