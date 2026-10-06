package com.asdflj.ae2thing.api.adapter.pattern;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public interface IDualInterfacePatternValidator {

    boolean accepts(ItemStack pattern, DualInterfacePatternTarget target, EntityPlayerMP player);
}
