package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;

import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.data.IAEStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Implemented by containers that host an {@link ITerminalTypeFilterProvider} terminal so the client can sync per-type
 * visibility toggles to the server.
 */
public interface ITypeFilterContainer {

    ITerminalTypeFilterProvider getTypeFilterHost();

    default void updateTypeFilters(Reference2BooleanMap<IAEStackType<?>> map, EntityPlayer player) {
        final ITerminalTypeFilterProvider host = this.getTypeFilterHost();
        if (host == null || map == null) {
            return;
        }
        final Reference2BooleanMap<IAEStackType<?>> target = host.getTypeFilter(player);
        for (Reference2BooleanMap.Entry<IAEStackType<?>> entry : map.reference2BooleanEntrySet()) {
            target.put(entry.getKey(), entry.getBooleanValue());
        }
        host.saveTypeFilter();
    }
}
