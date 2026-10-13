package com.asdflj.ae2thing.common.item;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.storage.CellInventoryHandler;
import com.asdflj.ae2thing.common.storage.ICellInventoryHandler;
import com.asdflj.ae2thing.common.storage.ITCellInventory;
import com.asdflj.ae2thing.common.storage.infinityCell.InfinityItemStorageCellInventory;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.util.NameConst;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEItemStackType;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemInfinityStorageCell extends BaseCellItem implements IStorageCell, IItemGroup {

    private final int perType = 1;

    @SuppressWarnings("Guava")
    public ItemInfinityStorageCell() {
        this.setMaxStackSize(1);
        this.setUnlocalizedName(NameConst.ITEM_INFINITY_CELL);
        this.setTextureName(
            AE2Thing.resource(NameConst.ITEM_INFINITY_CELL)
                .toString());
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
    }

    @Override
    public String getUnlocalizedGroupName(Set<ItemStack> otherItems, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        final IMEInventoryHandler<?> inventory = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(stack, null, AEItemStackType.ITEM_STACK_TYPE);

        if (inventory instanceof ICellInventoryHandler handler) {
            final ITCellInventory cellInventory = handler.getCellInv();

            if (cellInventory != null) {
                String uid = cellInventory.getUUID();
                if (!uid.isEmpty()) lines.add(uid);
                if (handler.isPreformatted()) {
                    String filter = cellInventory.getOreFilter();
                    if (filter.isEmpty()) {
                        final String list = (handler.getIncludeExcludeMode() == IncludeExclude.WHITELIST
                            ? GuiText.Included
                            : GuiText.Excluded).getLocal();

                        if (handler.isFuzzy()) {
                            lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Fuzzy.getLocal());
                        } else {
                            lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Precise.getLocal());
                        }
                        if (GuiScreen.isShiftKeyDown()) {
                            lines.add(GuiText.Filter.getLocal() + ": ");
                            IAEStackInventory inv = cellInventory.getConfigAEInventory();
                            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                                if (!(inv.getAEStackInSlot(i) instanceof IAEItemStack aes)) {
                                    continue;
                                }
                                ItemStack s = aes.getItemStack();
                                if (s != null) lines.add(s.getDisplayName());
                            }
                        }
                    } else {
                        lines.add(GuiText.PartitionedOre.getLocal() + " : " + filter);
                    }

                    if (handler.getSticky()) {
                        lines.add(GuiText.Sticky.getLocal());
                    }
                }
            }
        }
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getBytesLong(final ItemStack cellItem) {
        return (Long.MAX_VALUE - 9) / 8 + 1;
    }

    @Override
    public int BytePerType(ItemStack cellItem) {
        return this.perType;
    }

    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return this.perType;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isBlackListed(IAEStack<?> requestedAddition) {
        if (!(requestedAddition instanceof IAEItemStack is)) {
            return true;
        }

        return is.getItem() == null;
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public boolean isStorageCell(ItemStack i) {
        return false;
    }

    @Override
    public double getIdleDrain() {
        return 2000D;
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    @Override
    public IInventory getUpgradesInventory(ItemStack is) {
        return new CellUpgrades(is, 2);
    }

    @Override
    public IAEStackInventory getConfigAEInventory(ItemStack is) {
        return new CellConfig(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return FuzzyMode.fromItemStack(is);
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        ItemStackNBT.setString(is, "FuzzyMode", fzMode.name());
    }

    @Override
    public ItemInfinityStorageCell register() {
        GameRegistry.registerItem(this, NameConst.ITEM_INFINITY_CELL, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    @NotNull
    public IAEStackType<?> getStackType() {
        return AEItemStackType.ITEM_STACK_TYPE;
    }

    @Override
    public IMEInventoryHandler<IAEItemStack> getInventoryHandler(ItemStack o, ISaveProvider container,
        EntityPlayer player) throws AppEngException {
        return new CellInventoryHandler(new InfinityItemStorageCellInventory(o, container, player));
    }
}
