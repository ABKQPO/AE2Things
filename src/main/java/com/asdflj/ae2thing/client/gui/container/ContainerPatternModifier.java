package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.client.gui.container.slot.SlotEncodedPatternInput;
import com.asdflj.ae2thing.inventory.AEStackItemInventory;
import com.asdflj.ae2thing.inventory.item.PatternModifierInventory;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidEncodedPattern;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IVirtualSlotSource;
import appeng.container.slot.SlotRestrictedInput;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.StackInfo;

public class ContainerPatternModifier extends AEBaseContainer implements IPatternValueContainer, IVirtualSlotSource {

    private final PatternModifierInventory it;

    @Override
    public void updateVirtualSlot(StorageName name, int slotId, IAEStack<?> aes) {
        if (this.it == null) return;
        final IAEStackInventory inv = this.it.getAEInventoryByName(name);
        if (inv == null || inv.getSizeInventory() <= 0) return;
        if (aes == null) {
            inv.putAEStackInSlot(0, null);
            return;
        }
        final IAEStack<?> stored = aes.copy();
        stored.setStackSize(1);
        inv.putAEStackInSlot(0, stored);
    }

    @Override
    public StorageName getAEStorageName(Slot slot) {
        return null;
    }

    private final SlotRestrictedInput[] pattern = new SlotRestrictedInput[36];
    public final AEStackInventorySyncHandler replaceSourceSync;
    public final AEStackInventorySyncHandler replaceTargetSync;
    private static final ItemStack encodePattern = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .get();
    private static final ItemStack ultimatePattern = AEApi.instance()
        .definitions()
        .items()
        .encodedUltimatePattern()
        .maybeStack(1)
        .get();
    private final IInventory patterns;

    public ContainerPatternModifier(InventoryPlayer ip, ITerminalHost host) {
        super(ip, host);
        this.it = (PatternModifierInventory) host;
        if (this.it == null) {
            this.patterns = null;
            this.replaceSourceSync = null;
            this.replaceTargetSync = null;
            return;
        }
        this.patterns = this.it.getInventoryByName(Constants.PATTERN);
        final SyncRegistrar sync = this.syncRegistrar();
        final IAEStackInventory sourceInv = this.it.getAEInventoryByName(StorageName.CRAFTING_INPUT);
        final IAEStackInventory targetInv = this.it.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
        this.replaceSourceSync = sourceInv == null ? null : sync.aeStackInventory("replaceSource", sourceInv);
        this.replaceTargetSync = targetInv == null ? null : sync.aeStackInventory("replaceTarget", targetInv);
        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            int x = (i % 9) * 18 + 8;
            int y = (i / 9) * 18 + 19;
            this.addSlotToContainer(this.pattern[i] = new SlotEncodedPatternInput(this.patterns, i, x, y, ip));
        }
        this.lockPlayerInventorySlot(it.getInventorySlot());
        this.bindPlayerInventory(ip, 0, 125);
    }

    public ItemStack getReplaceSource() {
        return this.getReplaceStack(StorageName.CRAFTING_INPUT);
    }

    public ItemStack getReplaceTarget() {
        return this.getReplaceStack(StorageName.CRAFTING_OUTPUT);
    }

    private ItemStack getReplaceStack(StorageName name) {
        if (this.it == null) return null;
        final IAEStackInventory inv = this.it.getAEInventoryByName(name);
        return inv == null ? null : AEStackItemInventory.toItemStack(inv.getAEStackInSlot(0));
    }

    public void clearPattern() {
        int blankPattern = 0;
        for (int i = 0; i < this.patterns.getSizeInventory(); i++) {
            ItemStack itemStack = this.patterns.getStackInSlot(i);
            if (itemStack != null) {
                blankPattern++;
                this.patterns.setInventorySlotContents(i, null);
            }
        }
        if (blankPattern <= 0) return;
        ItemStack pattern = AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(blankPattern)
            .get();
        if (!getPlayerInv().addItemStackToInventory(pattern)) {
            this.dropItem(pattern);
        }
    }

    protected void dropItem(ItemStack is) {
        if (is == null || is.stackSize <= 0) return;
        ItemStack itemStack = is.copy();
        int i = itemStack.getMaxStackSize();
        while (itemStack.stackSize > 0) {
            if (i > itemStack.stackSize) {
                if (!getPlayerInv().addItemStackToInventory(itemStack.copy())) {
                    getPlayerInv().player.entityDropItem(itemStack.copy(), 0);
                }
                break;
            } else {
                itemStack.stackSize -= i;
                ItemStack item = itemStack.copy();
                item.stackSize = i;
                if (!getPlayerInv().addItemStackToInventory(item)) {
                    getPlayerInv().player.entityDropItem(item, 0);
                }
            }
        }
    }

    public void replacePattern() {
        final ItemStack source = this.getReplaceSource();
        if (source == null) return;
        final ItemStack target = this.getReplaceTarget();
        try {
            for (int i = 0; i < patterns.getSizeInventory(); i++) {
                ItemStack stack = patterns.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ICraftingPatternItem cpi) {
                    ICraftingPatternDetails details;
                    if (stack.getItem() instanceof ItemFluidEncodedPattern fluidEncodedPattern) {
                        details = fluidEncodedPattern
                            .getPatternForItem(stack, this.getInventoryPlayer().player.worldObj);
                    } else {
                        details = cpi.getPatternForItem(stack, this.getInventoryPlayer().player.worldObj);
                    }
                    IAEItemStack[] in = this.replacePattern(details.getInputs(), source, target, details);
                    IAEItemStack[] out = this.replacePattern(details.getOutputs(), source, target, details);
                    encode(details, in, out, i);

                }
            }
        } catch (Throwable ignored) {}
    }

    protected ItemStack stampAuthor(ItemStack patternStack) {
        if (patternStack.stackTagCompound == null) {
            patternStack.stackTagCompound = new NBTTagCompound();
        }
        patternStack.stackTagCompound.setString("author", this.getPlayerInv().player.getCommandSenderName());
        return patternStack;
    }

    private void encode(ICraftingPatternDetails cpi, IAEItemStack[] in, IAEItemStack[] out, int slot) {
        NBTTagList inList = list2tagList(in);
        NBTTagList outList = list2tagList(out);
        NBTTagCompound tag = (NBTTagCompound) Platform.openNbtData(cpi.getPattern())
            .copy();
        tag.setTag("in", inList);
        tag.setTag("out", outList);
        ItemStack cp = (cpi.isCraftable() ? encodePattern : ultimatePattern).copy();
        cp.setTagCompound(tag);
        patterns.setInventorySlotContents(slot, stampAuthor(cp));
    }

    private NBTTagList list2tagList(IAEItemStack[] list) {
        NBTTagList nbtTagList = new NBTTagList();
        for (IAEItemStack is : list) {
            if (is == null) {
                nbtTagList.appendTag(new NBTTagCompound());
            } else {
                final IAEStack<?> stack = toNativeStack(is);
                nbtTagList.appendTag(stack == null ? new NBTTagCompound() : stack.toNBTGeneric());
            }
        }
        return nbtTagList;
    }

    private IAEStack<?> toNativeStack(IAEStack<?> stack) {
        if (stack instanceof IAEItemStack item && ItemFluidDrop.isFluidStack(item.getItemStack())) {
            final FluidStack fs = ItemFluidDrop.getFluidStack(item.getItemStack());
            if (fs != null) {
                final IAEFluidStack fluid = AEFluidStack.create(fs);
                fluid.setStackSize(stack.getStackSize());
                return fluid;
            }
        }
        return stack;
    }

    private boolean isSameItem(ItemStack stack1, ItemStack stack2) {
        if (Util.isFluidPacket(stack1) || Util.isFluidPacket(stack2)) {
            FluidStack fs1 = StackInfo.getFluid(stack1);
            FluidStack fs2 = StackInfo.getFluid(stack2);
            if (fs1 != null && fs2 != null) {
                return fs1.getFluid()
                    .equals(fs2.getFluid());
            }
            return false;

        } else {
            return Platform.isSameItemPrecise(stack1, stack2);
        }
    }

    private IAEItemStack[] replacePattern(IAEItemStack[] list, ItemStack source, ItemStack target,
        ICraftingPatternDetails details) {
        IAEItemStack[] results = new IAEItemStack[list.length];
        for (int i = 0; i < list.length; i++) {
            IAEItemStack item = list[i];
            if (item == null) {
                results[i] = null;
                continue;
            }
            if (isSameItem(item.getItemStack(), source)) {
                if ((details.isCraftable() && target != null
                    && details.isValidItemForSlot(i, target, this.getPlayerInv().player.worldObj))
                    || (!details.isCraftable() && target != null)) {
                    if (Util.isFluidPacket(target)) {
                        IAEItemStack fluidDrop = ItemFluidDrop.newAeStack(ItemFluidPacket.getFluidStack(target));
                        if (fluidDrop != null) {
                            fluidDrop.setStackSize(item.getStackSize());
                        }
                        results[i] = fluidDrop;
                        continue;
                    }
                    IAEItemStack t = AEItemStack.create(target);
                    t.setStackSize(item.getStackSize());
                    results[i] = t;
                } else if (target == null && !details.isCraftable()) {
                    results[i] = null;
                } else {
                    results[i] = item;
                }
            } else {
                results[i] = item;
            }
        }
        return results;
    }

    protected NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();
        if (i != null) {
            Util.writeItemStackToNBT(i, c);
        }
        return c;
    }

    @Override
    public boolean isValidContainer() {
        return true;
    }
}
