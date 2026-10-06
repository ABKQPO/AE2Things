package com.asdflj.ae2thing.api.adapter.pattern;

import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;

public record DualInterfacePatternTarget(long id, IInventory patterns, World world, int numSlots) {}
