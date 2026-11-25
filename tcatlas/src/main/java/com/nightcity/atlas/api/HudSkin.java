package com.nightcity.atlas.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record HudSkin(
        Identifier era,
        Palette palette,
        Identifier frameTexture,
        int frameSlice,
        Identifier minimapMask,
        String compassStyle,
        boolean fontShadow,
        Identifier soundToast,
        Identifier soundAlert
) {
    public static final Codec<Integer> HEX_COLOR_CODEC = Codec.STRING.comapFlatMap(
            raw -> {
                String s = raw.startsWith("#") ? raw.substring(1) : raw;
                try {
                    long parsed = Long.parseUnsignedLong(s, 16);
                    if (s.length() == 6) {
                        parsed |= 0xFF000000L;
                    }
                    return DataResult.success((int) parsed);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Invalid hex color: " + raw);
                }
            },
            argb -> String.format("#%08X", argb)
    );

    public record Palette(
            int bgFill,
            int bgBorder,
            int textPrimary,
            int textSecondary,
            int accent,
            int ok,
            int warn,
            int danger
    ) {
        public static final Codec<Palette> CODEC = RecordCodecBuilder.create(i -> i.group(
                HEX_COLOR_CODEC.fieldOf("bg_fill").forGetter(Palette::bgFill),
                HEX_COLOR_CODEC.fieldOf("bg_border").forGetter(Palette::bgBorder),
                HEX_COLOR_CODEC.fieldOf("text_primary").forGetter(Palette::textPrimary),
                HEX_COLOR_CODEC.fieldOf("text_secondary").forGetter(Palette::textSecondary),
                HEX_COLOR_CODEC.fieldOf("accent").forGetter(Palette::accent),
                HEX_COLOR_CODEC.fieldOf("ok").forGetter(Palette::ok),
                HEX_COLOR_CODEC.fieldOf("warn").forGetter(Palette::warn),
                HEX_COLOR_CODEC.fieldOf("danger").forGetter(Palette::danger)
        ).apply(i, Palette::new));
    }

    public static final Codec<HudSkin> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("era").forGetter(HudSkin::era),
            Palette.CODEC.fieldOf("palette").forGetter(HudSkin::palette),
            Identifier.CODEC.fieldOf("frame_texture").forGetter(HudSkin::frameTexture),
            Codec.INT.optionalFieldOf("frame_slice", 4).forGetter(HudSkin::frameSlice),
            Identifier.CODEC.fieldOf("minimap_mask").forGetter(HudSkin::minimapMask),
            Codec.STRING.optionalFieldOf("compass_style", "notched_wood").forGetter(HudSkin::compassStyle),
            Codec.BOOL.optionalFieldOf("font_shadow", true).forGetter(HudSkin::fontShadow),
            Identifier.CODEC.fieldOf("sound_toast").forGetter(HudSkin::soundToast),
            Identifier.CODEC.fieldOf("sound_alert").forGetter(HudSkin::soundAlert)
    ).apply(i, HudSkin::new));

    public static HudSkin defaultStoneSkin() {
        return new HudSkin(
                Identifier.fromNamespaceAndPath("tctechnology", "stone"),
                new Palette(0xD82A241E, 0xFF6E5B45, 0xFFF2E8D5, 0xFFC7B89E, 0xFFD99B52, 0xFF6FA86B, 0xFFD9A441, 0xFFC85A4B),
                Identifier.fromNamespaceAndPath("tcatlas", "textures/gui/skin/stone_frame.png"),
                4,
                Identifier.fromNamespaceAndPath("tcatlas", "textures/gui/skin/round_rough.png"),
                "notched_wood",
                true,
                Identifier.fromNamespaceAndPath("tcatlas", "ui.toast.stone"),
                Identifier.fromNamespaceAndPath("tcatlas", "ui.alert.drum")
        );
    }

    public static double contrastRatio(int fgArgb, int bgArgb) {
        double l1 = relativeLuminance(fgArgb);
        double l2 = relativeLuminance(bgArgb);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int argb) {
        double r = linearize(((argb >> 16) & 0xFF) / 255.0);
        double g = linearize(((argb >> 8) & 0xFF) / 255.0);
        double b = linearize((argb & 0xFF) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
