package com.asdflj.ae2thing.loader;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.network.CPacketCraftRequest;
import com.asdflj.ae2thing.network.CPacketFindCellItem;
import com.asdflj.ae2thing.network.CPacketFluidUpdate;
import com.asdflj.ae2thing.network.CPacketInventoryAction;
import com.asdflj.ae2thing.network.CPacketInventoryActionExtend;
import com.asdflj.ae2thing.network.CPacketNEIRecipe;
import com.asdflj.ae2thing.network.CPacketNetworkCraftingItems;
import com.asdflj.ae2thing.network.CPacketOpenTerminal;
import com.asdflj.ae2thing.network.CPacketPatternNameSet;
import com.asdflj.ae2thing.network.CPacketPatternValueSet;
import com.asdflj.ae2thing.network.CPacketRenamer;
import com.asdflj.ae2thing.network.CPacketSwitchGuis;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;
import com.asdflj.ae2thing.network.CPacketTransferRecipe;
import com.asdflj.ae2thing.network.CPacketTypeFilter;
import com.asdflj.ae2thing.network.CPacketValueConfig;
import com.asdflj.ae2thing.network.SPacketCraftingDebugCardUpdate;
import com.asdflj.ae2thing.network.SPacketCraftingStateUpdate;
import com.asdflj.ae2thing.network.SPacketFindCellItem;
import com.asdflj.ae2thing.network.SPacketMEFluidInvUpdate;
import com.asdflj.ae2thing.network.SPacketMEItemInvUpdate;
import com.asdflj.ae2thing.network.SPacketNBTDataUpdate;
import com.asdflj.ae2thing.network.SPacketSetItemAmount;
import com.asdflj.ae2thing.network.SPacketSetItemName;
import com.asdflj.ae2thing.network.SPacketStringUpdate;
import com.asdflj.ae2thing.network.SPacketSwitchBack;
import com.asdflj.ae2thing.network.SPacketTypeFilter;
import com.asdflj.ae2thing.network.wrapper.AE2ThingNetworkWrapper;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;

public class ChannelLoader implements Runnable {

    public static final ChannelLoader INSTANCE = new ChannelLoader();
    private static final Class<?>[] MESSAGE_TYPES = { CPacketCraftRequest.class, CPacketFindCellItem.class,
        CPacketFluidUpdate.class, CPacketInventoryAction.class, CPacketInventoryActionExtend.class,
        CPacketNEIRecipe.class, CPacketNetworkCraftingItems.class, CPacketOpenTerminal.class,
        CPacketPatternNameSet.class, CPacketPatternValueSet.class, CPacketRenamer.class, CPacketSwitchGuis.class,
        CPacketTerminalBtns.class, CPacketTransferRecipe.class, CPacketTypeFilter.class, CPacketValueConfig.class,
        SPacketCraftingDebugCardUpdate.class, SPacketCraftingStateUpdate.class, SPacketFindCellItem.class,
        SPacketMEFluidInvUpdate.class, SPacketMEItemInvUpdate.class, SPacketNBTDataUpdate.class,
        SPacketSetItemAmount.class, SPacketSetItemName.class, SPacketStringUpdate.class, SPacketSwitchBack.class,
        SPacketTypeFilter.class };

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void run() {
        AE2ThingNetworkWrapper netHandler = AE2Thing.proxy.netHandler;
        for (int id = 0; id < MESSAGE_TYPES.length; id++) {
            Class<?> messageType = MESSAGE_TYPES[id];
            Class<?> handlerType = null;
            for (Class<?> nestedType : messageType.getDeclaredClasses()) {
                if (nestedType.getSimpleName()
                    .equals("Handler") && IMessageHandler.class.isAssignableFrom(nestedType)) {
                    handlerType = nestedType;
                    break;
                }
            }
            if (handlerType == null) {
                throw new IllegalStateException("Missing packet handler for " + messageType.getName());
            }
            try {
                IMessageHandler handler = (IMessageHandler) handlerType.getDeclaredConstructor()
                    .newInstance();
                netHandler.registerMessage(
                    handler,
                    (Class<? extends IMessage>) messageType,
                    id,
                    messageType.getSimpleName()
                        .startsWith("C") ? Side.SERVER : Side.CLIENT);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot register packet " + messageType.getName(), e);
            }
        }
    }

    public static void sendPacketToAllPlayers(Packet packet, World world) {
        for (Object player : world.playerEntities) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(packet);
            }
        }
    }
}
