package com.asdflj.ae2thing.util.enhancedsearch;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface RecipePages {

    default String baseMapId(String pageId) {
        return null;
    }
}
