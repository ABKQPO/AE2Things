package com.asdflj.ae2thing.coremod.mixin.nee;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.vfyjxf.nee.processor.BotaniaRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;

/**
 * inspired by <a href=
 * "https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/26601#issuecomment-5582181225">GTNewHorizons/GT-New-Horizons-Modpack#26601</a>
 * <p>
 * TODO: need to remove in future
 */
@Deprecated
@Mixin(value = BotaniaRecipeProcessor.class, remap = false)
public class MixinBotaniaRecipeProcessor {

    @Unique
    private static final Set<String> botfix$IDENTIFIERS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                RecipeProcessor.NULL_IDENTIFIER,
                "botania.petalApothecary",
                "botania.runicAltar",
                "botania.manaPool",
                "botania.pureDaisy",
                "botania.elvenTrade",
                "botania.brewery")));

    @Inject(method = "getAllOverlayIdentifier", at = @At("HEAD"), cancellable = true)
    private void modifyAllOverlayIdentifier(CallbackInfoReturnable<Set<String>> cir) {
        cir.setReturnValue(botfix$IDENTIFIERS);
    }
}
