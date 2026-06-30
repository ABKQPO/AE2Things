package com.asdflj.ae2thing.integration.ae2stuff;

import net.bdew.ae2stuff.misc.OverlayRenderHandler;
import net.bdew.ae2stuff.misc.WorldOverlayRenderer;

import com.asdflj.ae2thing.client.render.WirelessOverlayRender;

public class Ae2StuffClientIntegration {

    public static void registerOverlayRenderer() {
        OverlayRenderHandler.register(new Ae2StuffWirelessOverlayRenderer());
    }

    public static class Ae2StuffWirelessOverlayRenderer implements WorldOverlayRenderer {

        private final WirelessOverlayRender renderer = new WirelessOverlayRender();

        @Override
        public void doRender(float partialTicks, double viewX, double viewY, double viewZ) {
            this.renderer.doRender(partialTicks, viewX, viewY, viewZ);
        }
    }
}
