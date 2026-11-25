package com.nightcity.tcivilization.api;

import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;

/**
 * Handle to one loaded or cached region cell and its attached layers (03 §5.2.3, §12.1).
 */
public interface RegionHandle {
    RegionCell cell();

    <T> T layer(RegionLayer<T> layer);

    <T> void setLayer(RegionLayer<T> layer, T value);

    <T> void update(RegionLayer<T> layer, UnaryOperator<T> operator);

    void markDirty(Identifier layerId);

    boolean isDirty();

    Set<Identifier> dirtyLayers();
}
