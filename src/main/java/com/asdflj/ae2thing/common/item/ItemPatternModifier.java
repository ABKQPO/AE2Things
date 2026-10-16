package com.asdflj.ae2thing.common.item;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.PatternModifierModeObject;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.IItemInventory;
import com.asdflj.ae2thing.inventory.item.PatternModifierInventory;
import com.asdflj.ae2thing.util.BlockPos;
import com.asdflj.ae2thing.util.GTUtil;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.parts.IPart;
import appeng.api.util.IInterfaceViewable;
import appeng.tile.networking.TileCableBus;
import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;

public class ItemPatternModifier extends BaseItem implements IItemInventory {

    public ItemPatternModifier() {
        super();
        setUnlocalizedName(NameConst.ITEM_PATTERN_MODIFIER);
        setTextureName(
            AE2Thing.resource(NameConst.ITEM_PATTERN_MODIFIER)
                .toString());
    }

    @Override
    public ItemPatternModifier register() {
        GameRegistry.registerItem(this, NameConst.ITEM_PATTERN_MODIFIER, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack item, World w, EntityPlayer player) {
        if (player.isSneaking()) {
            if (!PatternModifierModeObject.isBlockUseFallback(player)) {
                PatternModifierModeObject.Mode mode = PatternModifierModeObject.nextMode(item);
                if (Platform.isClient()) {
                    player.addChatComponentMessage(
                        new ChatComponentText(
                            I18n.format(NameConst.PATTERN_MODIFIER_CURRENT_MODE) + " " + I18n.format(mode.langKey)));
                }
            }
        } else {
            InventoryHandler.openGui(
                player,
                w,
                new BlockPos(player.inventory.currentItem, 0, 0),
                ForgeDirection.UNKNOWN,
                this.guiGuiType());
        }
        return item;
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        lines.add(
            I18n.format(NameConst.PATTERN_MODIFIER_CURRENT_MODE) + " "
                + I18n.format(PatternModifierModeObject.getMode(stack).langKey));
        if (isShiftKeyDown()) {
            lines.addAll(
                Arrays.asList(
                    I18n.format(NameConst.TT_PATTERN_MODIFIER_DESC)
                        .split("\\\\n")));
        } else {
            lines.add(I18n.format(NameConst.TT_SHIFT_FOR_MORE));
        }
    }

    @Override
    public boolean onItemUseFirst(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return false;
        PatternModifierModeObject.markBlockUse(player);
        if (!Platform.isServer()) return false;
        TileEntity te = world.getTileEntity(x, y, z);
        PatternModifierInventory patternModifierInventory = new PatternModifierInventory(itemstack, player);
        IInterfaceViewable host;
        if (te instanceof IInterfaceViewable h) {
            host = h;
        } else if (Mods.isLegacyGt5Loaded() || Mods.isGt5UnofficialLoaded()) {
            host = GTUtil.getIInterfaceViewable(te);
        } else if (te instanceof TileCableBus bus) {
            Vec3 vec = Vec3.createVectorHelper(hitX, hitY, hitZ);
            IPart part = bus.selectPart(vec).part;
            if (part instanceof IInterfaceViewable) {
                host = (IInterfaceViewable) part;
            } else {
                host = null;
            }
        } else {
            host = null;
        }
        if (patternModifierInventory.extractToHost(host) && player instanceof EntityPlayerMP serverPlayer) {
            serverPlayer.playerNetServerHandler
                .sendPacket(new S29PacketSoundEffect("random.orb", player.posX, player.posY, player.posZ, 0.5F, 1.0F));
        }
        return true;
    }

    private GuiType guiGuiType() {
        return GuiType.PATTERN_MODIFIER;
    }

    @Override
    public Object getInventory(ItemStack stack, World world, int x, int y, int z, EntityPlayer player) {
        return new PatternModifierInventory(stack, x, player);
    }
}
