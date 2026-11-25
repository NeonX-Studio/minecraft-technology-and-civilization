package com.nightcity.wildlands.mixin;

import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.planet.PlanetCoordinates;
import com.nightcity.wildlands.planet.PlanetRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ClientTimeRemapMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true, require = 0)
    private void tcwildlands$remapTimeOfDay(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        if (!self.isClientSide() || self.dimension() != Level.OVERWORLD || !PlanetRuntime.isPlanetActive()) {
            return;
        }
        if (WildlandsConfig.CLIENT_SPEC.isLoaded() && !WildlandsConfig.CLIENT_SUN_REMAP.getAsBoolean()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            float remapped = PlanetCoordinates.remapClientTimeOfDay(
                    mc.player.getBlockX(), mc.player.getBlockZ(), self.getOverworldClockTime(), partialTick
            );
            cir.setReturnValue(remapped);
        }
    }
}
