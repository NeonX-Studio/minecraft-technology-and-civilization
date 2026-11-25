package com.nightcity.tcivilization.api.world;

import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Lithostratigraphic column of up to 8 layers at an (x, z) location (10 §5.10.2, §12.1).
 */
public record StrataColumn(GeoProvince province, List<Layer> layers) {
    public StrataColumn {
        province = province == null ? GeoProvince.CRATON_PLATFORM : province;
        layers = layers == null ? List.of() : List.copyOf(layers);
    }

    public record Layer(
            Identifier rock,
            int topY,
            int thicknessBlocks,
            float dipDirDeg,
            float dipAngleDeg
    ) {
    }

    public Identifier rockAtY(int y) {
        for (Layer layer : layers) {
            int bottomY = layer.topY() - layer.thicknessBlocks();
            if (y <= layer.topY() && y >= bottomY) {
                return layer.rock();
            }
        }
        return layers.isEmpty()
                ? Identifier.withDefaultNamespace("granite")
                : layers.getLast().rock();
    }
}
