package com.asdflj.ae2thing.common.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.parts.PartThaumatoriumInterface;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.util.NameConst;
import com.asdflj.ae2thing.util.Util;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Deprecated
public class ItemPartThaumatoriumInterface extends BaseItem implements IPartItem {

    public ItemPartThaumatoriumInterface() {
        this.setMaxStackSize(64);
        this.setUnlocalizedName(NameConst.ITEM_PART_THAUMATORIUM_INTERFACE);
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer(this);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float xOffset, float yOffset, float zOffset) {
        return AEApi.instance()
            .partHelper()
            .placeBus(player.getHeldItem(), x, y, z, side, player, world);
    }

    @Nullable
    @Override
    public PartThaumatoriumInterface createPartFromItemStack(ItemStack is) {
        return new PartThaumatoriumInterface(is);
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon(AE2Thing.MODID + ":" + NameConst.ITEM_PART_THAUMATORIUM_INTERFACE);
    }

    @Override
    public ItemPartThaumatoriumInterface register() {
        GameRegistry.registerItem(this, NameConst.ITEM_PART_THAUMATORIUM_INTERFACE, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
        if (Util.getAEVersion() >= 1080) {
            lines.add(StatCollector.translateToLocal("item.deprecation"));
        }
    }
}
