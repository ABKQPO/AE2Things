package com.asdflj.ae2thing.loader;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.util.BaublesUtil;
import com.asdflj.ae2thing.util.InvUtil;

public class InvLoader implements Runnable {

    @Override
    public void run() {
        InvUtil.INVENTORY.add(player -> player.inventory);
        if (Mods.BAUBLES.isModLoaded()) {
            InvUtil.INVENTORY.add(BaublesUtil::getBaublesInv);
        }
    }
}
