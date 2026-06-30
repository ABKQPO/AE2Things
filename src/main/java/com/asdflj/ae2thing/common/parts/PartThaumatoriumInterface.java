package com.asdflj.ae2thing.common.parts;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.asdflj.ae2thing.inventory.IEssentiaContainer;
import com.asdflj.ae2thing.util.AspectUtil;
import com.glodblock.github.common.parts.PartFluidInterface;

import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.api.tiles.IEssentiaTransportWithSimulate;
import thaumicenergistics.common.storage.AEEssentiaStack;

public class PartThaumatoriumInterface extends PartFluidInterface
    implements IEssentiaTransportWithSimulate, IEssentiaContainer {

    private final AspectList aspects = new AspectList();
    private final MachineSource source = new MachineSource(this);

    public PartThaumatoriumInterface(ItemStack is) {
        super(is);
    }

    @Override
    public void getDrops(List<ItemStack> drops, boolean wrenched) {
        super.getDrops(drops, wrenched);
        for (Aspect aspect : aspects.getAspectsSorted()) {
            int stored = aspects.getAmount(aspect);
            if (stored > 0) {
                // Essentia is no longer a fluid, so spill the buffered essentia as phial items.
                drops.add(ItemPhial.newStack(aspect, stored));
            }
        }
    }

    @Override
    public int addEssentia(@NotNull Aspect aspect, int amount, @NotNull ForgeDirection side, @NotNull Actionable mode) {
        if (amount <= 0) return 0;
        try {
            IMEMonitor<AEEssentiaStack> monitor = AspectUtil.getEssentiaMonitor(
                this.getProxy()
                    .getGrid());
            if (monitor == null) return 0;
            AEEssentiaStack notInsertable = monitor
                .injectItems(new AEEssentiaStack(aspect, amount), mode, this.source);
            if (notInsertable == null) return amount;
            return (int) (amount - notInsertable.getStackSize());
        } catch (Exception ignored) {}

        return 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection side) {
        return true;
    }

    @Override
    public boolean canInputFrom(ForgeDirection side) {
        return true;
    }

    @Override
    public boolean canOutputTo(ForgeDirection side) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int side) {

    }

    @Override
    public Aspect getSuctionType(ForgeDirection side) {
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection side) {
        return 8;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        int stored = aspects.getAmount(aspect);
        if (stored >= amount) {
            aspects.remove(aspect, amount);
            return amount;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return this.addEssentia(aspect, amount, side, Actionable.MODULATE);
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection side) {
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection side) {
        return 8;
    }

    @Override
    public int getMinimumSuction() {
        return 1;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    @Override
    public AspectList getAspects() {
        return this.aspects;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.aspects.readFromNBT(data);
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.aspects.writeToNBT(data);
    }
}
