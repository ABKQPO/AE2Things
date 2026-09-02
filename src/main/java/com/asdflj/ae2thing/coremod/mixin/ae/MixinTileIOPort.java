package com.asdflj.ae2thing.coremod.mixin.ae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.asdflj.ae2thing.coremod.hooker.CoreModHooks;

import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.storage.TileIOPort;

@Mixin(TileIOPort.class)
public abstract class MixinTileIOPort extends AENetworkInvTile {

    @ModifyVariable(
        method = "transferContents",
        at = @At(value = "HEAD"),
        remap = false,
        argsOnly = true,
        name = "itemsToMove")
    private long transferContents(long itemsToMove) {
        return CoreModHooks.getItemsToMove((TileIOPort) (Object) this, itemsToMove);
    }
}
