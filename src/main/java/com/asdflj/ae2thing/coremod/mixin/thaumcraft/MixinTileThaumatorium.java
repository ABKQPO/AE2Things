package com.asdflj.ae2thing.coremod.mixin.thaumcraft;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.asdflj.ae2thing.coremod.hooker.CoreModHooks;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.tiles.TileThaumatorium;

@Mixin(value = TileThaumatorium.class, remap = false)
public abstract class MixinTileThaumatorium {

    @Redirect(
        method = "fill()V",
        at = @At(
            value = "INVOKE",
            target = "Lthaumcraft/api/ThaumcraftApiHelper;getConnectableTile(Lnet/minecraft/world/World;IIILnet/minecraftforge/common/util/ForgeDirection;)Lnet/minecraft/tileentity/TileEntity;"),
        remap = false)
    private TileEntity ae2thing$getConnectableTile(World world, int x, int y, int z, ForgeDirection face) {
        TileEntity result = ThaumcraftApiHelper.getConnectableTile(world, x, y, z, face);
        TileThaumatorium tile = (TileThaumatorium) (Object) this;
        CoreModHooks.getConnectableTile(tile, y - tile.yCoord, face);
        return result;
    }
}
