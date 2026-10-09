package com.asdflj.ae2thing.inventory.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.api.WirelessObject;
import com.asdflj.ae2thing.inventory.AEStackItemInventory;
import com.asdflj.ae2thing.inventory.ItemBiggerAppEngInventory;
import com.asdflj.ae2thing.inventory.ItemPatternRefillInventory;
import com.asdflj.ae2thing.inventory.ItemPatternsInventory;
import com.asdflj.ae2thing.inventory.SlotBackedAEStackInventory;
import com.asdflj.ae2thing.util.Util;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.parts.IInterfaceTerminal;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalTypeFilterProvider;
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
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import appeng.util.TerminalSettings;
import appeng.util.item.AEItemStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public class WirelessDualInterfaceTerminalInventory extends WirelessTerminal
    implements IGridHost, com.asdflj.ae2thing.inventory.IPatternTerminal, appeng.api.parts.IPatternTerminal,
    IClickableInTerminal, IAEAppEngInventory, IInterfaceTerminal, ITerminalTypeFilterProvider {

    private static final String PANEL_POSITIONS_TAG = "ae2thingPanelPositions";
    private static final int MIN_PANEL_POSITION = -10000;
    private static final int MAX_PANEL_POSITION = 10000;

    protected AEStackItemInventory craftingEx;
    protected AEStackItemInventory outputEx;
    protected AppEngInternalInventory pattern;
    protected AppEngInternalInventory upgrades;
    protected AppEngInternalInventory crafting;
    protected boolean craftingMode = false;
    protected boolean substitute = false;
    protected boolean combine = false;
    protected boolean prioritize = false;
    protected boolean inverted = false;
    protected boolean beSubstitute = false;
    protected int activePage = 0;
    private Util.DimensionalCoordSide tile;
    private final TerminalSettings terminalSettings = new TerminalSettings();
    private final IInventory viewCells = new AppEngInternalInventory(null, 5);
    private final IAEStackInventory craftingView;
    private final Set<PatternEncodeListener> encodeListeners = Collections.newSetFromMap(new WeakHashMap<>());

    public WirelessDualInterfaceTerminalInventory(WirelessObject obj) {
        super(obj);
        pattern = new ItemPatternsInventory(obj.getItemStack(), this, obj.getPlayer(), obj.getSlot());
        crafting = new ItemBiggerAppEngInventory(
            obj.getItemStack(),
            Constants.CRAFTING,
            9,
            obj.getPlayer(),
            obj.getSlot(),
            this);
        craftingEx = new AEStackItemInventory(
            obj.getItemStack(),
            Constants.CRAFTING_EX,
            32,
            obj.getPlayer(),
            obj.getSlot(),
            StorageName.CRAFTING_INPUT);
        outputEx = new AEStackItemInventory(
            obj.getItemStack(),
            Constants.OUTPUT_EX,
            32,
            obj.getPlayer(),
            obj.getSlot(),
            StorageName.CRAFTING_OUTPUT);
        this.craftingView = new SlotBackedAEStackInventory(this.crafting, StorageName.CRAFTING_INPUT);
        upgrades = new ItemPatternRefillInventory(
            obj.getItemStack(),
            Constants.UPGRADES,
            1,
            1,
            obj.getPlayer(),
            obj.getSlot());
        this.readFromNBT();
    }

    private void readFromNBT() {
        NBTTagCompound data = Platform.openNbtData(this.obj.getItemStack());
        this.setSubstitution(data.getBoolean("substitute"));
        this.setCombineMode(data.getBoolean("combine"));
        this.setBeSubstitute(data.getBoolean("beSubstitute"));
        this.setPrioritization(data.getBoolean("priorization"));
        this.setInverted(data.getBoolean("inverted"));
        this.setActivePage(data.getInteger("activePage"));
        this.setCraftingRecipe(data.getBoolean("craftingMode"));
        if (data.hasKey("clickedInterface")) {
            NBTTagCompound tileMsg = (NBTTagCompound) data.getTag("clickedInterface");
            this.tile = Util.DimensionalCoordSide.readFromNBT(tileMsg);
        }
        this.terminalSettings.readFromNBT(data);
    }

    @Override
    public IConfigManager getConfigManager() {
        final ConfigManager out = new ConfigManager((manager, settingName, newValue) -> {
            final NBTTagCompound data = Platform.openNbtData(this.getItemStack());
            manager.writeToNBT(data);
            saveSettings();
        });
        out.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        out.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        out.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        out.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.getItemStack())
                .copy());
        return out;
    }

    @Override
    public IGridNode getActionableNode() {
        return this.obj.getGridNode();
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return this.obj.getGridNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return null;
    }

    @Override
    public void securityBreak() {

    }

    public IAEStackInventory getCraftingExInventory() {
        return this.craftingEx.getAEInventory();
    }

    public IAEStackInventory getOutputExInventory() {
        return this.outputEx.getAEInventory();
    }

    @Override
    public IInventory getInventoryByName(String name) {
        return switch (name) {
            case Constants.CRAFTING -> this.crafting;
            case Constants.PATTERN -> this.pattern;
            case Constants.UPGRADES -> this.upgrades;
            default -> null;
        };

    }

    @Override
    public void setActivePage(int value) {
        this.activePage = value;
    }

    @Override
    public int getActivePage() {
        return this.activePage;
    }

    @Override
    public boolean shouldCombine() {
        return this.combine;
    }

    @Override
    public void setCombineMode(boolean shouldCombine) {
        this.combine = shouldCombine;
    }

    @Override
    public void setPrioritization(boolean canPrioritize) {
        this.prioritize = canPrioritize;
    }

    @Override
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public void setCraftingRecipe(boolean craftingMode) {
        this.craftingMode = craftingMode;
        this.fixCraftingRecipes();
    }

    @Override
    public void setSubstitution(boolean canSubstitute) {
        this.substitute = canSubstitute;
    }

    @Override
    public void setBeSubstitute(boolean canBeSubstitute) {
        this.beSubstitute = canBeSubstitute;
    }

    @Override
    public boolean isCraftingRecipe() {
        return this.craftingMode;
    }

    @Override
    public boolean isInverted() {
        return this.inverted;
    }

    @Override
    public boolean canBeSubstitute() {
        return this.beSubstitute;
    }

    @Override
    public boolean isPrioritize() {
        return this.prioritize;
    }

    @Override
    public boolean isSubstitution() {
        return this.substitute;
    }

    @Override
    public void sortCraftingItems() {
        List<IAEStack<?>> items = new ArrayList<>();
        List<IAEStack<?>> fluids = new ArrayList<>();
        for (int i = 0; i < this.craftingEx.getSizeInventory(); i++) {
            final IAEStack<?> stack = this.craftingEx.getAEStackInSlot(i);
            if (stack == null) continue;
            if (stack instanceof IAEFluidStack) {
                fluids.add(stack);
            } else {
                items.add(stack);
            }
        }
        final List<IAEStack<?>> ordered = new ArrayList<>(items.size() + fluids.size());
        if (this.prioritize) {
            ordered.addAll(fluids);
        }
        ordered.addAll(items);
        if (!this.prioritize) {
            ordered.addAll(fluids);
        }
        for (int i = 0; i < this.craftingEx.getSizeInventory(); i++) {
            this.craftingEx.putAEStackInSlot(i, i < ordered.size() ? ordered.get(i) : null);
        }
    }

    @Override
    public void saveSettings() {
        writeToNBT();
    }

    public NBTTagCompound getPanelPositions() {
        NBTTagCompound data = Platform.openNbtData(this.nbtSource());
        if (!data.hasKey(PANEL_POSITIONS_TAG)) {
            return new NBTTagCompound();
        }
        NBTTagCompound stored = data.getCompoundTag(PANEL_POSITIONS_TAG);
        NBTTagCompound positions = new NBTTagCompound();
        copyPanelPosition(stored, positions, "patternX");
        copyPanelPosition(stored, positions, "patternY");
        copyPanelPosition(stored, positions, "itemX");
        copyPanelPosition(stored, positions, "itemY");
        return positions;
    }

    private ItemStack nbtSource() {
        ItemStack cached = this.getItemStack();
        EntityPlayer player = this.obj.getPlayer();
        if (player != null) {
            ItemStack live = player.inventory.getStackInSlot(this.obj.getSlot());
            if (live != null && live.getItem() == cached.getItem()) return live;
        }
        return cached;
    }

    public void setPanelPositions(NBTTagCompound positions) {
        if (positions == null) return;
        NBTTagCompound stored = this.getPanelPositions();
        copyPanelPosition(positions, stored, "patternX");
        copyPanelPosition(positions, stored, "patternY");
        copyPanelPosition(positions, stored, "itemX");
        copyPanelPosition(positions, stored, "itemY");
        NBTTagCompound data = Platform.openNbtData(this.nbtSource());
        data.setTag(PANEL_POSITIONS_TAG, stored);
        this.saveSettings();
    }

    private static void copyPanelPosition(NBTTagCompound source, NBTTagCompound target, String key) {
        if (source.hasKey(key)) {
            target.setInteger(key, clampPanelPosition(source.getInteger(key)));
        }
    }

    private static int clampPanelPosition(int value) {
        return Math.max(MIN_PANEL_POSITION, Math.min(MAX_PANEL_POSITION, value));
    }

    @Override
    public boolean hasRefillerUpgrade() {
        return upgrades.getStackInSlot(0) != null;
    }

    @Override
    public void saveAEStackInv() {
        this.craftingEx.saveAEStackInv();
        this.outputEx.saveAEStackInv();
        this.writeToNBT();
    }

    @Override
    public IAEStackInventory getAEInventoryByName(StorageName name) {
        if (this.craftingMode) {
            return name == StorageName.CRAFTING_INPUT ? this.craftingView : null;
        }
        return switch (name) {
            case CRAFTING_INPUT -> this.getCraftingExInventory();
            case CRAFTING_OUTPUT -> this.getOutputExInventory();
            default -> null;
        };
    }

    @Override
    public boolean canBeSubstitution() {
        return this.beSubstitute;
    }

    @Override
    public void setCanBeSubstitution(boolean beSubstitute) {
        this.beSubstitute = beSubstitute;
    }

    @Override
    public IInventory getViewCellStorage() {
        return this.viewCells;
    }

    @Override
    public PinsHandler getPinsHandler(EntityPlayer player) {
        return null;
    }

    @Override
    public IMEMonitor<?> getMEMonitor(IAEStackType<?> type) {
        return type == AEItemStackType.ITEM_STACK_TYPE ? this.getItemInventory() : this.getFluidInventory();
    }

    @Override
    public void exPatternTerminalCall(IAEStack<?>[] in, IAEStack<?>[] out) {}

    @Override
    public Iterable<PatternEncodeListener> getPatternEncodeListeners() {
        return this.encodeListeners;
    }

    @Override
    public void addPatternEncodeListeners(PatternEncodeListener listener) {
        this.encodeListeners.add(listener);
    }

    @Override
    public void removePatternEncodeListeners(PatternEncodeListener listener) {
        this.encodeListeners.remove(listener);
    }

    @Override
    public boolean encode(IEnergySource powerSource, IMEMonitor<IAEItemStack> itemMonitor,
        BaseActionSource actionSource, String auther, World world) {
        return PatternEncodingHelper.encode(this, powerSource, itemMonitor, actionSource, auther, world);
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {}

    @Override
    public void saveChanges() {

    }

    @Override
    public void onChangeInventory(IInventory inv, int slot, InvOperation mc, ItemStack removedStack,
                                  ItemStack newStack) {
        if (inv == this.pattern && slot == 1) {
            final ItemStack is = inv.getStackInSlot(1);

            if (is != null && is.getItem() instanceof final ICraftingPatternItem craftingPatternItem) {
                final ICraftingPatternDetails details = craftingPatternItem
                    .getPatternForItem(is, this.getActionableNode().getWorld());

                if (!this.applyPatternFromItem(is) && details != null) {
                    this.applyPatternContents(
                        details.getAEInputs(),
                        details.getAEOutputs(),
                        details.isCraftable(),
                        details.canSubstitute(),
                        newStack != null && details.canBeSubstitute());
                }
            }
        }
        if (inv == this.crafting) {
            this.fixCraftingRecipes();
        }
    }

    private void applyPatternContents(IAEStack<?>[] inItems, IAEStack<?>[] outItems, boolean craftable,
        boolean canSubstitute, boolean canBeSubstitute) {
        this.setCraftingRecipe(craftable);
        int inputsCount = 0;
        int outputCount = 0;
        for (IAEStack<?> inItem : inItems) {
            if (inItem != null) {
                inputsCount++;
            }
        }
        for (IAEStack<?> outItem : outItems) {
            if (outItem != null) {
                outputCount++;
            }
        }
        this.setSubstitution(canSubstitute);
        this.setBeSubstitute(canBeSubstitute);
        this.setInverted(inputsCount <= 8 && outputCount > 8);
        this.setActivePage(0);

        for (int i = 0; i < this.craftingEx.getSizeInventory(); i++) {
            this.craftingEx.putAEStackInSlot(i, null);
        }
        for (int i = 0; i < this.outputEx.getSizeInventory(); i++) {
            this.outputEx.putAEStackInSlot(i, null);
        }
        for (int i = 0; i < this.crafting.getSizeInventory(); i++) {
            this.crafting.setInventorySlotContents(i, null);
        }

        if (this.isCraftingRecipe()) {
            for (int i = 0; i < this.crafting.getSizeInventory() && i < inItems.length; i++) {
                if (inItems[i] instanceof IAEItemStack item) {
                    this.crafting.setInventorySlotContents(i, item.getItemStack());
                }
            }
        } else {
            for (int i = 0; i < this.craftingEx.getSizeInventory() && i < inItems.length; i++) {
                if (inItems[i] != null) {
                    this.craftingEx.putAEStackInSlot(i, inItems[i]);
                }
            }
        }

        if (this.inverted) {
            for (int i = 0; i < this.outputEx.getSizeInventory() && i < outItems.length; i++) {
                if (outItems[i] != null) {
                    this.outputEx.putAEStackInSlot(i, outItems[i]);
                }
            }
        } else {
            for (int i = 0; i < outItems.length && i < 8; i++) {
                if (outItems[i] != null) {
                    this.outputEx.putAEStackInSlot(i >= 4 ? 12 + i : i, outItems[i]);
                }
            }
        }
    }

    private boolean applyPatternFromItem(ItemStack pattern) {
        final NBTTagCompound encoded = pattern.getTagCompound();
        if (encoded == null) return false;
        final IAEStack<?>[] inItems = readGenericStacks(encoded.getTagList("in", NBT.TAG_COMPOUND));
        final IAEStack<?>[] outItems = readGenericStacks(encoded.getTagList("out", NBT.TAG_COMPOUND));
        if (inItems == null && outItems == null) return false;
        this.applyPatternContents(
            inItems == null ? new IAEStack<?>[0] : inItems,
            outItems == null ? new IAEStack<?>[0] : outItems,
            encoded.getBoolean("crafting"),
            encoded.getBoolean("substitute"),
            encoded.getBoolean("beSubstitute"));
        return true;
    }

    private static IAEStack<?>[] readGenericStacks(NBTTagList tags) {
        boolean any = false;
        final IAEStack<?>[] stacks = new IAEStack<?>[tags.tagCount()];
        for (int i = 0; i < tags.tagCount(); i++) {
            final IAEStack<?> stack = IAEStack.fromNBTGeneric(tags.getCompoundTagAt(i));
            stacks[i] = stack;
            if (stack != null) {
                any = true;
            }
        }
        return any ? stacks : null;
    }

    private void fixCraftingRecipes() {
        if (this.craftingMode) {
            for (int x = 0; x < this.crafting.getSizeInventory(); x++) {
                final ItemStack is = this.crafting.getStackInSlot(x);
                if (is != null) {
                    is.stackSize = 1;
                }
            }
        }
    }

    private void writeToNBT() {
        NBTTagCompound data = Platform.openNbtData(this.getItemStack());
        data.setBoolean("craftingMode", this.craftingMode);
        data.setBoolean("substitute", this.substitute);
        data.setBoolean("combine", this.combine);
        data.setBoolean("beSubstitute", this.beSubstitute);
        data.setBoolean("priorization", this.prioritize);
        data.setBoolean("inverted", this.inverted);
        data.setInteger("activePage", this.activePage);
        this.craftingEx.saveAEStackInv();
        this.outputEx.saveAEStackInv();
        this.upgrades.markDirty();
        this.pattern.markDirty();
        this.crafting.markDirty();
        NBTTagCompound tileMsg = new NBTTagCompound();
        if (tile != null) {
            tile.writeToNBT(tileMsg);
        }
        data.setTag("clickedInterface", tileMsg);
        this.terminalSettings.writeToNBT(data);
    }

    @Override
    public void setClickedInterface(Util.DimensionalCoordSide tile) {
        this.tile = tile;
        this.writeToNBT();
    }

    @Override
    public Util.DimensionalCoordSide getClickedInterface() {
        return this.tile;
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    @Override
    public Reference2BooleanMap<IAEStackType<?>> getTypeFilter(EntityPlayer player) {
        return this.terminalSettings.getFilters(player)
            .getFiltersMap();
    }

    @Override
    public void saveTypeFilter() {
        this.writeToNBT();
    }
}
