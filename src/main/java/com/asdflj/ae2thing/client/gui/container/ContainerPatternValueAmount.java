package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.client.gui.GuiPatternItemRenamer;
import com.asdflj.ae2thing.client.gui.GuiPatternValueAmount;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.api.config.SecurityPermissions;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerSubGui;
import appeng.container.PrimaryGui;
import appeng.container.interfaces.IVirtualSlotSource;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;
import appeng.util.Platform;

public class ContainerPatternValueAmount extends ContainerSubGui implements IVirtualSlotSource {

    private StorageName invName;
    private IAEStack<?> aes;
    private int slotIndex;

    public ContainerPatternValueAmount(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    public static PrimaryGui primaryGui(GuiType guiType, ItemStack icon, BlockPos pos, ForgeDirection side) {
        return new AE2ThingPrimaryGui(guiType, icon, pos, side);
    }

    @Override
    public void updateVirtualSlot(StorageName invName, int slotId, IAEStack<?> aes) {
        this.invName = invName;
        this.aes = aes;
        this.slotIndex = slotId;

        if (Platform.isServer()) {
            for (ICrafting crafter : this.crafters) {
                NetworkHandler.instance
                    .sendTo(new PacketPatternValueSet(aes, invName, slotId), (EntityPlayerMP) crafter);
            }
        } else {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (gs instanceof GuiPatternValueAmount gpva) {
                gpva.update();
            } else if (gs instanceof GuiPatternItemRenamer gpir) {
                gpir.update();
            }
        }
    }

    public StorageName getInvName() {
        return this.invName;
    }

    public IAEStack<?> getAEStack() {
        return this.aes;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.verifyPermissions(SecurityPermissions.CRAFT, false);
    }

    @Override
    public boolean isValidContainer() {
        return true;
    }

    private static final class AE2ThingPrimaryGui extends PrimaryGui {

        private final GuiType guiType;
        private final BlockPos pos;
        private final ForgeDirection side;

        private AE2ThingPrimaryGui(GuiType guiType, ItemStack icon, BlockPos pos, ForgeDirection side) {
            super(guiType, icon, null, side);
            this.guiType = guiType;
            this.pos = pos;
            this.side = side;
        }

        @Override
        public void open(EntityPlayer player) {
            if (player.worldObj.isRemote || this.guiType == null) return;
            InventoryHandler.openGui(player, player.worldObj, this.pos, this.side, this.guiType);
        }
    }
}
