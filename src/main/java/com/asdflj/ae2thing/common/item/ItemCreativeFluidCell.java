package com.asdflj.ae2thing.common.item;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.storage.FluidCellInventoryHandler;
import com.asdflj.ae2thing.common.storage.ITFluidCellInventory;
import com.asdflj.ae2thing.common.storage.ITFluidCellInventoryHandler;
import com.asdflj.ae2thing.common.storage.infinityCell.CreativeFluidCellInventory;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.util.NameConst;
import com.glodblock.github.api.FluidCraftAPI;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.core.features.AEFeature;
import appeng.items.contents.CellUpgrades;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.item.AEFluidStackType;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.registry.GameRegistry;

public class ItemCreativeFluidCell extends BaseCellItem implements IStorageCell {

    public static final ItemStack water_bucket = new ItemStack(Items.water_bucket, 1);
    public static final ItemStack lava_bucket = new ItemStack(Items.lava_bucket, 1);
    private final int perType = 1;
    private final double idleDrain = 2000D;

    @Override
    public IMEInventoryHandler<IAEFluidStack> getInventoryHandler(ItemStack o, ISaveProvider container,
        EntityPlayer player) throws AppEngException {
        return new FluidCellInventoryHandler(new CreativeFluidCellInventory(o, container, player));
    }

    protected static class InfinityConfig extends AppEngInternalInventory {

        public InfinityConfig(final ItemStack is) {
            super(null, 1);
            this.setInventorySlotContents(0, is);
        }

        @Override
        public void markDirty() {}
    }

    private final ItemStack is;
    private final String name;

    public ItemCreativeFluidCell(String name, ItemStack is) {
        this.is = is;
        this.name = name;
        FluidStack fs = StackInfo.getFluid(is);
        setUnlocalizedName(this.name);
        setTextureName(
            AE2Thing
                .resource(
                    String.format(
                        "%s_%s",
                        NameConst.ITEM_CREATIVE_FLUID_CELL,
                        fs.getFluid()
                            .getName()))
                .toString());
        this.setMaxStackSize(1);
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
    }

    @Override
    public ItemCreativeFluidCell register() {
        GameRegistry.registerItem(this, this.name, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int BytePerType(ItemStack cellItem) {
        return perType;
    }

    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return perType;
    }

    @Override
    public boolean isBlackListed(IAEStack<?> requestedAddition) {
        if (!(requestedAddition instanceof IAEFluidStack fluidStack)) {
            return true;
        }
        return fluidStack.getFluid() == null || FluidCraftAPI.instance()
            .isBlacklistedInStorage(
                fluidStack.getFluid()
                    .getClass());
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
        return idleDrain;
    }

    @Override
    public double getIdleDrain(ItemStack is) {
        return idleDrain;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return 1;
    }

    @Override
    public IAEStackType<?> getStackType() {
        return AEFluidStackType.FLUID_STACK_TYPE;
    }

    @Override
    public boolean isEditable(ItemStack is) {
        return false;
    }

    @Override
    public IInventory getUpgradesInventory(ItemStack is) {
        return new CellUpgrades(is, 0);
    }

    @Override
    public IInventory getConfigInventory(ItemStack is) {
        return new InfinityConfig(this.is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return null;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {

    }

    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines,
                                      final boolean displayMoreInfo) {
        final IMEInventoryHandler<?> inventory = AEApi.instance().registries().cell()
            .getCellInventory(stack, null, StorageChannel.FLUIDS);

        if (inventory instanceof final ITFluidCellInventoryHandler handler) {
            final ITFluidCellInventory cellInventory = handler.getCellInv();

            if (GuiScreen.isCtrlKeyDown()) {
                if (!cellInventory.getContents().isEmpty()) {
                    lines.add(StatCollector.translateToLocal(NameConst.TT_CELL_CONTENTS));
                    for (IAEFluidStack fluid : cellInventory.getContents()) {
                        if (fluid != null) {
                            lines.add(String.format("  %s", fluid.getFluidStack().getLocalizedName()));
                        }
                    }
                } else {
                    lines.add(StatCollector.translateToLocal(NameConst.TT_CELL_EMPTY));
                }
            } else {
                lines.add(StatCollector.translateToLocal(NameConst.TT_CTRL_FOR_MORE));
            }
        }
    }

}
