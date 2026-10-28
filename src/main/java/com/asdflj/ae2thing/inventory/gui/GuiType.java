package com.asdflj.ae2thing.inventory.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.client.gui.GuiCellLink;
import com.asdflj.ae2thing.client.gui.GuiCraftAmount;
import com.asdflj.ae2thing.client.gui.GuiCraftConfirm;
import com.asdflj.ae2thing.client.gui.GuiCraftingStatus;
import com.asdflj.ae2thing.client.gui.GuiCraftingTerminal;
import com.asdflj.ae2thing.client.gui.GuiFluidPacketEncoder;
import com.asdflj.ae2thing.client.gui.GuiInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.GuiManaIO;
import com.asdflj.ae2thing.client.gui.GuiPatternItemRenamer;
import com.asdflj.ae2thing.client.gui.GuiPatternModifier;
import com.asdflj.ae2thing.client.gui.GuiPatternValueAmount;
import com.asdflj.ae2thing.client.gui.GuiRenamer;
import com.asdflj.ae2thing.client.gui.GuiTerminalMenu;
import com.asdflj.ae2thing.client.gui.GuiUniversalIO;
import com.asdflj.ae2thing.client.gui.GuiUniversalStorageBus;
import com.asdflj.ae2thing.client.gui.GuiWirelessConnectorTerminal;
import com.asdflj.ae2thing.client.gui.GuiWirelessDistributor;
import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.AE2ThingPrimaryGui;
import com.asdflj.ae2thing.client.gui.container.ContainerCellLink;
import com.asdflj.ae2thing.client.gui.container.ContainerCraftConfirm;
import com.asdflj.ae2thing.client.gui.container.ContainerCraftingTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerFluidPacketEncoder;
import com.asdflj.ae2thing.client.gui.container.ContainerInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerManaIO;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternItemRenamer;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternModifier;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.ae2thing.client.gui.container.ContainerRenamer;
import com.asdflj.ae2thing.client.gui.container.ContainerTerminalMenu;
import com.asdflj.ae2thing.client.gui.container.ContainerUniversalIO;
import com.asdflj.ae2thing.client.gui.container.ContainerUniversalStorageBus;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessConnectorTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDistributor;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.common.parts.SharedManaBus;
import com.asdflj.ae2thing.common.parts.THPart;
import com.asdflj.ae2thing.common.parts.universal.PartUniversalExportBus;
import com.asdflj.ae2thing.common.parts.universal.PartUniversalImportBus;
import com.asdflj.ae2thing.common.parts.universal.PartUniversalStorageBus;
import com.asdflj.ae2thing.common.tile.TileFluidPacketEncoder;
import com.asdflj.ae2thing.common.tile.TileWirelessDistributor;
import com.asdflj.ae2thing.inventory.ItemCellLinkInventory;
import com.asdflj.ae2thing.util.BlockPos;
import com.google.common.collect.ImmutableList;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiPriority;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.container.implementations.ContainerPriority;
import appeng.helpers.IPrimaryGuiIconProvider;
import appeng.helpers.IPriorityHost;
import appeng.parts.automation.PartSharedItemBus;

public enum GuiType {

    BACKPACK_TERMINAL(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftingTerminal(player.inventory, inv);
        }
    }),
    PATTERN_MODIFIER(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternModifier(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternModifier(player.inventory, inv);
        }
    }),
    TERMINAL_MENU(new NullGuiFactory() {

        @Override
        public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
            return new ContainerTerminalMenu(player.inventory);
        }

        @Override
        public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
            return new GuiTerminalMenu();
        }
    }),
    WIRELESS_CONNECTOR_TERMINAL(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerWirelessConnectorTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiWirelessConnectorTerminal(player.inventory, inv);
        }
    }),
    WIRELESS_CONNECTOR_TERMINAL_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerWirelessConnectorTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiWirelessConnectorTerminal(player.inventory, inv);
        }
    }),
    WIRELESS_DUAL_INTERFACE_TERMINAL(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerWirelessDualInterfaceTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiWirelessDualInterfaceTerminal(player.inventory, inv);
        }
    }),
    INFUSION_PATTERN_TERMINAL(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerInfusionPatternTerminal(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiInfusionPatternTerminal(player.inventory, inv);
        }
    }),
    CRAFTING_CONFIRM(new PartGuiFactory<>(THPart.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, THPart inv) {
            return new ContainerCraftConfirm(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, THPart inv) {
            return new GuiCraftConfirm(player.inventory, inv);
        }
    }),
    CRAFTING_CONFIRM_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftConfirm(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftConfirm(player.inventory, inv);
        }
    }),
    RENAMER(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerRenamer(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiRenamer(player.inventory, inv);
        }
    }),
    CRAFTING_STATUS(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingStatus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftingStatus(player.inventory, inv);
        }
    }),
    CRAFTING_STATUS_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftingStatus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftingStatus(player.inventory, inv);
        }
    }),
    PATTERN_VALUE_SET(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueAmount(player.inventory, inv);
        }
    }),
    PATTERN_VALUE_SET_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternValueAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternValueAmount(player.inventory, inv);
        }
    }),
    PATTERN_NAME_SET(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternItemRenamer(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternItemRenamer(player.inventory, inv);
        }
    }),
    PATTERN_NAME_SET_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerPatternItemRenamer(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiPatternItemRenamer(player.inventory, inv);
        }
    }),

    CRAFTING_AMOUNT(new PartGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftAmount(player.inventory, inv);
        }
    }),
    CRAFTING_AMOUNT_ITEM(new ItemGuiFactory<>(ITerminalHost.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ITerminalHost inv) {
            return new ContainerCraftAmount(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ITerminalHost inv) {
            return new GuiCraftAmount(player.inventory, inv);
        }
    }),
    MANA_BUS_IO(new PartGuiFactory<>(SharedManaBus.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, SharedManaBus inv) {
            return new ContainerManaIO(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, SharedManaBus inv) {
            return new GuiManaIO(player.inventory, inv);
        }
    }),
    CELL_LINK(new ItemGuiFactory<>(ItemCellLinkInventory.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, ItemCellLinkInventory inv) {
            return new ContainerCellLink(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, ItemCellLinkInventory inv) {
            return new GuiCellLink(player.inventory, inv);
        }
    }),
    FLUID_PACKET_ENCODER(new TileGuiFactory<>(TileFluidPacketEncoder.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, TileFluidPacketEncoder inv) {
            return new ContainerFluidPacketEncoder(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, TileFluidPacketEncoder inv) {
            return new GuiFluidPacketEncoder(player.inventory, inv);
        }
    }),
    WIRELESS_DISTRIBUTOR(new TileGuiFactory<>(TileWirelessDistributor.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, TileWirelessDistributor inv) {
            return new ContainerWirelessDistributor(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, TileWirelessDistributor inv) {
            return new GuiWirelessDistributor(player.inventory, inv);
        }
    }),
    WCT_CRAFTING_TERMINAL_BRIDGE(new OptionalWCTGuiFactory()),
    UNIVERSAL_BUS(new PartGuiFactory<>(PartSharedItemBus.class) {

        @Override
        protected PartSharedItemBus<?> getInventory(TileEntity tile, ForgeDirection face) {
            if (!(super.getInventory(tile, face) instanceof PartSharedItemBus<?>bus)) return null;
            return bus instanceof PartUniversalImportBus || bus instanceof PartUniversalExportBus ? bus : null;
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected Object createServerGui(EntityPlayer player, PartSharedItemBus inv) {
            return new ContainerUniversalIO(player.inventory, inv);
        }

        @Override
        @SuppressWarnings("rawtypes")
        protected Object createClientGui(EntityPlayer player, PartSharedItemBus inv) {
            return new GuiUniversalIO(player.inventory, inv);
        }
    }),
    UNIVERSAL_STORAGE_BUS(new PartGuiFactory<>(PartUniversalStorageBus.class) {

        @Override
        protected Object createServerGui(EntityPlayer player, PartUniversalStorageBus inv) {
            return new ContainerUniversalStorageBus(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, PartUniversalStorageBus inv) {
            return new GuiUniversalStorageBus(player.inventory, inv);
        }
    }),
    PRIORITY(new PartGuiFactory<>(IPriorityHost.class) {

        @Override
        public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
            if (!(super.createServerGui(player, world, x, y, z, face) instanceof ContainerPriority container)) {
                return null;
            }
            final TileEntity te = world.getTileEntity(x, y, z);
            if (!(te instanceof IPartHost host)) return container;
            final IPart part = host.getPart(face);
            final GuiType returnGui = returnGuiOf(part);
            final ItemStack icon = part instanceof IPrimaryGuiIconProvider provider ? provider.getPrimaryGuiIcon()
                : null;
            if (returnGui != null && icon != null) {
                container.setPrimaryGui(AE2ThingPrimaryGui.create(returnGui, icon, new BlockPos(te), face));
            }
            return container;
        }

        @Override
        protected Object createServerGui(EntityPlayer player, IPriorityHost inv) {
            return new ContainerPriority(player.inventory, inv);
        }

        @Override
        protected Object createClientGui(EntityPlayer player, IPriorityHost inv) {
            return new GuiPriority(player.inventory, inv);
        }

        private static GuiType returnGuiOf(final IPart part) {
            if (part instanceof PartUniversalStorageBus) return UNIVERSAL_STORAGE_BUS;
            return null;
        }
    });

    public static final List<GuiType> VALUES = ImmutableList.copyOf(values());

    @Nullable
    public static GuiType getByOrdinal(int ordinal) {
        return ordinal < 0 || ordinal >= VALUES.size() ? null : VALUES.get(ordinal);
    }

    public final IGuiFactory guiFactory;

    GuiType(IGuiFactory guiFactory) {
        this.guiFactory = guiFactory;
    }
}
