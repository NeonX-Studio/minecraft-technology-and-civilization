package com.nightcity.tcivilization.client.api;

import com.nightcity.tcivilization.TCivilization;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * UI skin contract and built-in neutral skin tcivilization:skin/neutral (03 §5.10.9, 31 §10.2).
 */
public interface TcSkin {
    Identifier NEUTRAL_ID = Identifier.fromNamespaceAndPath(TCivilization.MOD_ID, "skin/neutral");
    int NINE_SLICE_PADDING = 6;

    enum ColorblindMode {
        OFF,
        DEUTERANOPIA,
        PROTANOPIA,
        TRITANOPIA,
        MONOCHROME
    }

    record Palette(
            int background,
            int panel,
            int text,
            int textMuted,
            int accent,
            int positive,
            int warning,
            int critical,
            int icon,
            int border
    ) {
        public boolean meetsContrastRequirements() {
            return contrastRatio(text, background) >= 4.5D
                    && contrastRatio(text, panel) >= 4.5D
                    && contrastRatio(warning, panel) >= 3.0D
                    && contrastRatio(critical, panel) >= 3.0D;
        }

        public Palette withColorblindMode(ColorblindMode mode) {
            return switch (mode) {
                case OFF -> this;
                case DEUTERANOPIA, PROTANOPIA -> new Palette(
                        background, panel, text, textMuted, accent,
                        0xFF005AB5, 0xFF8A5A00, 0xFFA63603, icon, border
                );
                case TRITANOPIA -> new Palette(
                        background, panel, text, textMuted, accent,
                        0xFF006D77, 0xFF8A4F00, 0xFFA12622, icon, border
                );
                case MONOCHROME -> new Palette(
                        background, panel, text, textMuted, 0xFF3A3A3A,
                        0xFF2B2B2B, 0xFF444444, 0xFF141414, icon, border
                );
            };
        }
    }

    Palette NEUTRAL_PALETTE = new Palette(
            0xFFE6E6E3,
            0xFFD4D4D0,
            0xFF1F1F1F,
            0xFF555555,
            0xFF3B5B7A,
            0xFF2E6B30,
            0xFF8A5A00,
            0xFFA12622,
            0xFF2A2A2A,
            0xFF4A4A4A
    );

    TcSkin NEUTRAL = new SimpleSkin(NEUTRAL_ID, NEUTRAL_PALETTE, false);

    Identifier id();

    Palette palette();

    default int borderThickness(boolean highContrast) {
        return highContrast ? 2 : 1;
    }

    default int nineSlicePadding() {
        return NINE_SLICE_PADDING;
    }

    boolean animatedEffectsEnabled();

    static TcSkin active() {
        return Holder.active;
    }

    static void setActive(TcSkin skin) {
        Holder.active = Objects.requireNonNull(skin, "skin");
    }

    static double contrastRatio(int argbA, int argbB) {
        double l1 = relativeLuminance(argbA);
        double l2 = relativeLuminance(argbB);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05D) / (darker + 0.05D);
    }

    private static double relativeLuminance(int argb) {
        double r = linearize(((argb >> 16) & 0xFF) / 255.0D);
        double g = linearize(((argb >> 8) & 0xFF) / 255.0D);
        double b = linearize((argb & 0xFF) / 255.0D);
        return 0.2126D * r + 0.7152D * g + 0.0722D * b;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928D ? (channel / 12.92D) : Math.pow((channel + 0.055D) / 1.055D, 2.4D);
    }

    record SimpleSkin(Identifier id, Palette palette, boolean animatedEffectsEnabled) implements TcSkin {
        public SimpleSkin {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(palette, "palette");
        }
    }

    final class Holder {
        private static volatile TcSkin active = NEUTRAL;
    }
}
