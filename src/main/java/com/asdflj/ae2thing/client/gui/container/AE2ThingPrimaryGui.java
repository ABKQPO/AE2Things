package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.container.PrimaryGui;

public final class AE2ThingPrimaryGui extends PrimaryGui {

    private final GuiType guiType;
    private final BlockPos pos;
    private final ForgeDirection side;

    private AE2ThingPrimaryGui(GuiType guiType, ItemStack icon, BlockPos pos, ForgeDirection side) {
        super(guiType, icon, null, side);
        this.guiType = guiType;
        this.pos = pos;
        this.side = side;
    }

    public static PrimaryGui create(GuiType guiType, ItemStack icon, BlockPos pos, ForgeDirection side) {
        return new AE2ThingPrimaryGui(guiType, icon, pos, side);
    }

    @Override
    public void open(EntityPlayer player) {
        if (player.worldObj.isRemote || this.guiType == null) return;
        InventoryHandler.openGui(player, player.worldObj, this.pos, this.side, this.guiType);
    }
}
