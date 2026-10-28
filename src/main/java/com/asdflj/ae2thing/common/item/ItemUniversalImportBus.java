package com.asdflj.ae2thing.common.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.parts.universal.PartUniversalImportBus;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemUniversalImportBus extends BaseItem implements IPartItem {

    public ItemUniversalImportBus() {
        this.setMaxStackSize(64);
        this.setUnlocalizedName(NameConst.ITEM_PART_UNIVERSAL_IMPORT);
        AEApi.instance()
            .partHelper()
            .setItemBusRenderer(this);
    }

    @Nullable
    @Override
    public PartUniversalImportBus createPartFromItemStack(ItemStack is) {
        return new PartUniversalImportBus(is);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float xOffset, float yOffset, float zOffset) {
        return AEApi.instance()
            .partHelper()
            .placeBus(player.getHeldItem(), x, y, z, side, player, world);
    }

    @Override
    public ItemUniversalImportBus register() {
        GameRegistry.registerItem(this, NameConst.ITEM_PART_UNIVERSAL_IMPORT, AE2Thing.MODID);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    public void registerIcons(IIconRegister _iconRegister) {
        this.itemIcon = _iconRegister.registerIcon(AE2Thing.MODID + ":" + NameConst.ITEM_PART_UNIVERSAL_IMPORT);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }
}
