package com.asdflj.ae2thing.client.gui.widget;

import appeng.api.storage.data.IAEStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Implemented by terminal GUIs that own a {@link TypeFilterWidget} so the server can push the authoritative per-type
 * visibility map after the container opens.
 */
public interface ITypeFilterGui {

    void updateTypeFilters(Reference2BooleanMap<IAEStackType<?>> map);
}
