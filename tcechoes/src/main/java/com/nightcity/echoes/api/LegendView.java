package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Read-only view of a civilization's known legend instance and clue state (18 §5.6, §11.2, §12.1).
 */
public record LegendView(
        int seedId,
        UUID civId,
        State state,
        BlockPos rumourCenter,
        int rumourRadius,
        Identifier sourceId,
        long heardTick
) {
    public enum State {
        RUMOR,
        LOCATED,
        EXCAVATED,
        INTERPRETED,
        EXPLAINED;

        public static final Codec<State> CODEC = Codec.STRING.xmap(
                s -> State.valueOf(s.toUpperCase(Locale.ROOT)),
                st -> st.name().toLowerCase(Locale.ROOT)
        );
    }

    @FunctionalInterface
    public interface LegendSource {
        double errorFactor();
    }

    public static final Codec<LegendView> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("seed_id").forGetter(LegendView::seedId),
            UUIDUtil.CODEC.fieldOf("civ_id").forGetter(LegendView::civId),
            State.CODEC.fieldOf("state").forGetter(LegendView::state),
            BlockPos.CODEC.fieldOf("rumour_center").forGetter(LegendView::rumourCenter),
            Codec.INT.fieldOf("rumour_radius").forGetter(LegendView::rumourRadius),
            Identifier.CODEC.fieldOf("source_id").forGetter(LegendView::sourceId),
            Codec.LONG.fieldOf("heard_tick").forGetter(LegendView::heardTick)
    ).apply(i, LegendView::new));

    public LegendView {
        Objects.requireNonNull(civId, "civId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rumourCenter, "rumourCenter");
        rumourRadius = Math.clamp(rumourRadius, 64, 512);
        Objects.requireNonNull(sourceId, "sourceId");
    }
}
