package com.asdflj.ae2thing.util;

import static com.asdflj.ae2thing.api.Constants.MessageType.UPDATE_PLAYER_ITEM;
import static com.asdflj.ae2thing.nei.NEI_TH_Config.getConfigValue;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.ButtonConstants;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.asdflj.ae2thing.network.SPacketMEItemInvUpdate;

import appeng.api.util.IInterfaceViewable;
import appeng.util.item.AEItemStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.blocks.ItemMachines;
import gregtech.nei.GTNEIDefaultHandler;

public class GTUtil {

    public static IInterfaceViewable getIInterfaceViewable(TileEntity tile) {
        if (tile instanceof BaseMetaTileEntity bmte && bmte.getMetaTileEntity() instanceof IInterfaceViewable iv) {
            return iv;
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isDataStick() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack dataStick = player.inventory.getItemStack();
        return ItemList.Tool_DataStick.isStackEqual(dataStick, false, true);
    }

    @SideOnly(Side.CLIENT)
    public static String getRecipeName(IRecipeHandler recipe, List<OrderStack<?>> in) {
        if (!(recipe instanceof GTNEIDefaultHandler)) return null;

        String head = null;
        try {
            head = recipe.getOverlayIdentifier();
        } catch (Throwable ignored) {}
        if (head == null || head.trim()
            .isEmpty()) head = recipe.getRecipeTabName();
        if (head == null) return null;

        StringBuilder suggestion = new StringBuilder();
        if (getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_ENHANCED_SEARCH)) {
            suggestion.append('@')
                .append(head.trim());
        } else {
            suggestion.append(head.trim());
        }

        if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()
            && getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT)
            && PHUtil.isProgrammingToolkitEnabled()) {
            return suggestion.toString();
        }

        boolean hasZeroSizeInput = false;
        for (OrderStack<?> stack : in) {
            if (stack.getStack() instanceof ItemStack is && is.stackSize == 0) {
                hasZeroSizeInput = true;
                break;
            }
        }

        if (hasZeroSizeInput && Mods.hasGTNonConsumedDisplay()) {
            boolean appendCircuit = getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE);
            boolean appendNonConsumedItems = getConfigValue(
                ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_NON_CONSUMED_ITEMS);
            for (String token : GTNonConsumedHelper.collectTokens(recipe, in, appendCircuit, appendNonConsumedItems)) {
                suggestion.append(" ")
                    .append(token);
            }
        }

        return suggestion.toString();
    }

    public static void setDataStick(int x, int y, int z, EntityPlayer player, World w) {
        TileEntity tile = w.getTileEntity(x, y, z);
        if (tile instanceof BaseMetaTileEntity bmte && bmte.getMetaTileEntity() instanceof IInterfaceViewable) {
            ItemStack dataStick = player.inventory.getItemStack();
            if (ItemList.Tool_DataStick.isStackEqual(dataStick, false, true)) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("type", "CraftingInputBuffer");
                tag.setInteger("x", x);
                tag.setInteger("y", y);
                tag.setInteger("z", z);
                dataStick.setTagCompound(tag);
                dataStick
                    .setStackDisplayName("Crafting Input Buffer Link Data Stick (" + x + ", " + y + ", " + z + ")");
                player.addChatMessage(new ChatComponentText("Saved Link Data to Data Stick"));
                SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
                packet.appendItem(AEItemStack.create(dataStick));
                AE2Thing.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
            }
        }
    }

    public static boolean isHatchItem(ItemStack item) {
        if (item != null && item.getItem() instanceof ItemMachines) {
            return ItemMachines.getMetaTileEntity(item) instanceof MTEHatch;
        }
        return false;
    }

}
