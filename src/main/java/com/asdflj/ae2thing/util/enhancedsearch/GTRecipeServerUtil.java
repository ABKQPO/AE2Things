package com.asdflj.ae2thing.util.enhancedsearch;

import java.lang.reflect.Field;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.integration.Mods;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.util.IInterfaceViewable;
import appeng.helpers.IInterfaceHost;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.RecipeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class GTRecipeServerUtil {

    private GTRecipeServerUtil() {}

    public static String activeMapId(IInterfaceViewable host) {
        if (host == null || !Mods.hasGTRecipeMapWorkable()) return null;
        try {
            return Backend.resolve(host);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class Backend {

        private static final Long2ObjectMap<ChunkCoordinates> CONTROLLERS = new Long2ObjectOpenHashMap<>();
        private static Field ownerField;

        private static String resolve(IInterfaceViewable host) {
            final TileEntity tile = host.getTileEntity();
            if (tile == null || tile.getWorldObj() == null) return null;
            final World world = tile.getWorldObj();
            final long key = keyOf(world, tile);
            final ChunkCoordinates cached = CONTROLLERS.get(key);
            if (cached != null) {
                final RecipeMapWorkable workable = workableAt(world, cached);
                if (workable != null) {
                    final String mapId = mapIdOf(workable);
                    if (mapId != null) return mapId;
                }
                CONTROLLERS.remove(key);
            }
            final ChunkCoordinates[] found = new ChunkCoordinates[1];
            final String mapId = walk(world, tile, host, found);
            if (mapId != null && found[0] != null) CONTROLLERS.put(key, found[0]);
            return mapId;
        }

        private static long keyOf(World world, TileEntity tile) {
            final int dim = world.provider == null ? 0 : world.provider.dimensionId;
            return ((long) (dim & 0x3F) << 56) | ((long) (tile.xCoord & 0xFFFFFF) << 32)
                | ((long) (tile.zCoord & 0xFFFFFF) << 8)
                | (tile.yCoord & 0xFF);
        }

        private static RecipeMapWorkable workableAt(World world, ChunkCoordinates coords) {
            final TileEntity tile = world.getTileEntity(coords.posX, coords.posY, coords.posZ);
            if (!(tile instanceof IGregTechTileEntity gregTechTile)) return null;
            final IMetaTileEntity machine = gregTechTile.getMetaTileEntity();
            return machine instanceof RecipeMapWorkable workable ? workable : null;
        }

        private static String walk(World world, TileEntity tile, IInterfaceViewable host, ChunkCoordinates[] found) {
            if (!(host instanceof IInterfaceHost interfaceHost)) return fromTile(world, tile, found);
            final IGridNode node = interfaceHost.getGridNode(ForgeDirection.UNKNOWN);
            final IGrid grid = node == null ? null : node.getGrid();
            for (ForgeDirection direction : interfaceHost.getTargets()) {
                final TileEntity target = world.getTileEntity(
                    tile.xCoord + direction.offsetX,
                    tile.yCoord + direction.offsetY,
                    tile.zCoord + direction.offsetZ);
                if (target == null || target == tile) continue;
                if (target instanceof IInterfaceHost other && isOnSameGrid(other, grid)) continue;
                final String mapId = fromTile(world, target, found);
                if (mapId != null) return mapId;
            }
            return null;
        }

        private static boolean isOnSameGrid(IInterfaceHost other, IGrid grid) {
            if (grid == null) return false;
            final IGridNode otherNode = other.getGridNode(ForgeDirection.UNKNOWN);
            return otherNode != null && otherNode.getGrid() == grid;
        }

        private static String fromTile(World world, TileEntity tile, ChunkCoordinates[] found) {
            if (!(tile instanceof IGregTechTileEntity gregTechTile)) return null;
            final IMetaTileEntity machine = gregTechTile.getMetaTileEntity();
            if (machine instanceof RecipeMapWorkable workable) {
                found[0] = new ChunkCoordinates(tile.xCoord, tile.yCoord, tile.zCoord);
                return mapIdOf(workable);
            }
            if (!(machine instanceof MTEHatch hatch)) return null;
            final ChunkCoordinates owner = ownerCoords(hatch);
            if (owner == null) return null;
            final TileEntity controllerTile = world.getTileEntity(owner.posX, owner.posY, owner.posZ);
            if (!(controllerTile instanceof IGregTechTileEntity gregTechController)) return null;
            final IMetaTileEntity controller = gregTechController.getMetaTileEntity();
            if (!(controller instanceof RecipeMapWorkable workable)) return null;
            found[0] = owner;
            return mapIdOf(workable);
        }

        private static String mapIdOf(RecipeMapWorkable workable) {
            final RecipeMap<?> active = workable.getRecipeMap();
            return active == null || active.unlocalizedName == null || active.unlocalizedName.isEmpty() ? null
                : active.unlocalizedName;
        }

        private static ChunkCoordinates ownerCoords(MTEHatch hatch) {
            try {
                Field field = ownerField;
                if (field == null) {
                    field = MTEHatch.class.getDeclaredField("ae2CraftingIconOwner");
                    field.setAccessible(true);
                    ownerField = field;
                }
                return (ChunkCoordinates) field.get(hatch);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
