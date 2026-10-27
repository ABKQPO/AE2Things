package com.asdflj.ae2thing.common.block;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.common.tile.TileInfusionInterface;
import com.asdflj.ae2thing.util.NameConst;
import com.asdflj.ae2thing.util.Util;
import com.glodblock.github.common.block.BlockFluidInterface;

import appeng.block.AEBaseItemBlock;
import cpw.mods.fml.common.registry.GameRegistry;

@Deprecated
public class BlockInfusionInterface extends BlockFluidInterface {

    public BlockInfusionInterface() {
        super();
        setTileEntity(TileInfusionInterface.class);
        this.setBlockName(NameConst.BLOCK_INFUSION_INTERFACE);
        this.setBlockTextureName(AE2Thing.MODID + ":" + NameConst.BLOCK_INFUSION_INTERFACE);
    }

    @Override
    public BlockInfusionInterface register() {
        GameRegistry.registerBlock(this, AEBaseItemBlock.class, NameConst.BLOCK_INFUSION_INTERFACE);
        GameRegistry.registerTileEntity(TileInfusionInterface.class, NameConst.BLOCK_INFUSION_INTERFACE);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List<String> lines, boolean advancedItemTooltips) {
        super.addInformation(is, player, lines, advancedItemTooltips);
        if (Util.getAEVersion() >= 1080) {
            lines.add(StatCollector.translateToLocal("item.deprecation"));
        }
    }
}
