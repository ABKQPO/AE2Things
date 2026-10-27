package com.asdflj.ae2thing.common.block;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.item.BaseItemBlock;
import com.asdflj.ae2thing.common.tabs.AE2ThingTabs;
import com.asdflj.ae2thing.common.tile.TileEssentiaDiscretizer;
import com.asdflj.ae2thing.loader.IRegister;
import com.asdflj.ae2thing.util.NameConst;
import com.asdflj.ae2thing.util.Util;

import cpw.mods.fml.common.registry.GameRegistry;

@Deprecated
public class BlockEssentiaDiscretizer extends BaseTileBlock implements IRegister<BlockEssentiaDiscretizer> {

    public BlockEssentiaDiscretizer() {
        super(Material.iron);
        this.setBlockName(NameConst.BLOCK_ESSENTIA_DISCRETIZER);
        this.setBlockTextureName(AE2Thing.MODID + ":" + NameConst.BLOCK_ESSENTIA_DISCRETIZER);
        setFullBlock(true);
        setOpaque(true);
        setTileEntity(TileEssentiaDiscretizer.class);
    }

    public void setOpaque(boolean opaque) {
        this.isOpaque = opaque;
    }

    public void setFullBlock(boolean full) {
        this.isFullSize = full;
    }

    @Override
    public BlockEssentiaDiscretizer register() {
        GameRegistry.registerBlock(this, BaseItemBlock.class, NameConst.BLOCK_ESSENTIA_DISCRETIZER);
        GameRegistry.registerTileEntity(TileEssentiaDiscretizer.class, NameConst.BLOCK_ESSENTIA_DISCRETIZER);
        setCreativeTab(AE2ThingTabs.INSTANCE);
        return this;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean displayMoreInfo) {
        super.addInformation(stack, player, lines, displayMoreInfo);
        if (Util.getAEVersion() >= 1080) {
            lines.add(StatCollector.translateToLocal("item.deprecation"));
        }
    }

}
