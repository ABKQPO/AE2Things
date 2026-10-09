package com.asdflj.ae2thing.client.gui;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.asdflj.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.asdflj.ae2thing.client.render.ISlotRender;
import com.asdflj.ae2thing.client.render.SlotRender;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotInaccessible;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.StackInfo;

public interface IGuiDrawSlot {

    ThreadLocal<Boolean> DRAWING_SLOT = ThreadLocal.withInitial(() -> false);

    default IAEStack<?> getAEStackView(Slot slot, ItemStack drawStack) {
        IAEStack<?> encoded = this.getItemEncodedAEStackView(drawStack);
        if (encoded != null) return encoded;
        IAEItemStack view = slot instanceof SlotPatternFake slotFake ? slotFake.getAEStack()
            : slot instanceof SlotFakeCraftingMatrix slotFake ? slotFake.getAEStack() : AEItemStack.create(drawStack);
        return view != null && view.getItem() == null ? null : view;
    }

    default IAEStack<?> getItemEncodedAEStackView(ItemStack drawStack) {
        if (drawStack == null || drawStack.getItem() == null) {
            return null;
        }

        FluidStack neiFluid = StackInfo.getFluid(drawStack);
        if (neiFluid != null) {
            IAEFluidStack fluid = AEFluidStack.create(neiFluid);
            if (drawStack.getItem() instanceof ItemFluidDisplay display) {
                fluid.setStackSize(Math.max(display.getAmountLong(drawStack), 0));
            }
            return fluid;
        }

        if (drawStack.getItem() instanceof IAEFluidStack fStack) {
            return fStack;
        }

        if (drawStack.getItem() instanceof ItemFluidDrop) {
            FluidStack fluid = ItemFluidDrop.getFluidStack(drawStack);
            return fluid == null ? null : AEFluidStack.create(fluid);
        }

        if (drawStack.getItem() instanceof ItemFluidPacket) {
            return ItemFluidPacket.getFluidAEStack(drawStack);
        }

        return null;
    }

    default boolean drawSlot(Slot slot, Runnable baseDraw) {
        if (DRAWING_SLOT.get()) {
            baseDraw.run();
            return false;
        }
        DRAWING_SLOT.set(true);

        try {
            final ItemStack drawStack = slot.getStack();
            if (drawStack == null || drawStack.getItem() == null) return true;

            final IAEStack<?> stackView = this.getAEStackView(slot, drawStack);
            if (stackView == null) return true;

            boolean display = false;
            if (slot instanceof SlotInaccessible) {
                drawStack.stackSize = 0;
                ((SlotInaccessible) slot).setDisplay(true);
                display = true;
            } else if (!(slot instanceof SlotPatternFake || slot instanceof SlotPlayerInv
                || slot instanceof SlotPlayerHotBar
                || slot instanceof SlotFakeCraftingMatrix)) {
                    return true;
                }

            boolean result = true;
            for (ISlotRender slotRender : SlotRender.instance()
                .getRenders()) {
                if (!slotRender.get()
                    .test(slot)) {
                    continue;
                }

                if (!slotRender.drawSlot(slot, stackView, this, display)) {
                    result = false;
                    break;
                }
            }
            if (result) {
                baseDraw.run();
            }

            for (ISlotRender slotRender : SlotRender.instance()
                .getRenders()) {
                if (slotRender.get()
                    .test(slot)) {
                    slotRender.drawCallback(slot, stackView, this, display);
                }
            }
            return false; // always is false;
        } finally {
            DRAWING_SLOT.set(false);
        }
    }

    default void renderStackSize(boolean display, IAEItemStack stack, Slot slot) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!display) {
            GL11.glTranslatef(0.0f, 0.0f, 200.0f);
            aeRenderItem.renderItemOverlayIntoGUI(
                mc.fontRenderer,
                mc.getTextureManager(),
                stack.getItemStack(),
                slot.xDisplayPosition,
                slot.yDisplayPosition);
            GL11.glTranslatef(0.0f, 0.0f, -200.0f);
        }
    }

    default FontRenderer getFontRender() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    default void drawWidget(int posX, int posY, Fluid fluid) {
        if (fluid == null) return;
        IIcon icon = fluid.getIcon();
        if (icon == null) return;

        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glTranslatef(0f, 0f, 100.0f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor3f(
            (fluid.getColor() >> 16 & 0xFF) / 255.0F,
            (fluid.getColor() >> 8 & 0xFF) / 255.0F,
            (fluid.getColor() & 0xFF) / 255.0F);
        getAEBaseGui().drawTexturedModelRectFromIcon(posX, posY, fluid.getIcon(), 16, 16);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor3f(1, 1, 1);
        GL11.glTranslatef(0.0f, 0.0f, -100.0f);
    }

    AEBaseGui getAEBaseGui();

    float getzLevel();

}
