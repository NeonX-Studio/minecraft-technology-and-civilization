package com.nightcity.wildlands.mixin;

import com.nightcity.tcivilization.api.VanillaPolicyService;
import com.nightcity.tcivilization.api.VanillaSystem;
import com.nightcity.tcivilization.api.world.PrecipType;
import com.nightcity.tcivilization.api.world.TcWorldApis;
import com.nightcity.wildlands.WildlandsConfig;
import com.nightcity.wildlands.planet.PlanetRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true, require = 0)
    private void tcwildlands$isRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (self.dimension() != Level.OVERWORLD || !PlanetRuntime.isPlanetActive()) {
            return;
        }
        if (WildlandsConfig.SERVER_SPEC.isLoaded() && !WildlandsConfig.WEATHER_ENABLED.getAsBoolean()) {
            return;
        }
        if (VanillaPolicyService.get().owner(VanillaSystem.WEATHER).filter("tcwildlands"::equals).isEmpty()) {
            return;
        }
        PrecipType pt = TcWorldApis.weather().precipType(self, pos);
        cir.setReturnValue(pt == PrecipType.RAIN || pt == PrecipType.SLEET || pt == PrecipType.HAIL);
    }
}
