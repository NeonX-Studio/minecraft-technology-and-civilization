package com.nightcity.echoes.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Simulated ancient culture originating in the Dawn phase (18 §5.1.4, §11.1).
 */
public record Culture(
        int id,
        String nameKey,
        String displayName,
        LanguageTemplate language,
        ScriptType script,
        ArchStyle primaryStyle,
        ArchStyle secondaryStyle,
        Burial burial,
        Motif motif,
        Deity deity,
        long hearthRegion
) {
    public static final Codec<Culture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Culture::id),
            Codec.STRING.fieldOf("name_key").forGetter(Culture::nameKey),
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(Culture::displayName),
            LanguageTemplate.CODEC.fieldOf("language").forGetter(Culture::language),
            ScriptType.CODEC.fieldOf("script").forGetter(Culture::script),
            ArchStyle.CODEC.fieldOf("primary_style").forGetter(Culture::primaryStyle),
            ArchStyle.CODEC.fieldOf("secondary_style").forGetter(Culture::secondaryStyle),
            Burial.CODEC.fieldOf("burial").forGetter(Culture::burial),
            Motif.CODEC.fieldOf("motif").forGetter(Culture::motif),
            Deity.CODEC.fieldOf("deity").forGetter(Culture::deity),
            Codec.LONG.fieldOf("hearth_region").forGetter(Culture::hearthRegion)
    ).apply(i, Culture::new));

    public Culture {
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(primaryStyle, "primaryStyle");
        Objects.requireNonNull(secondaryStyle, "secondaryStyle");
        Objects.requireNonNull(burial, "burial");
        Objects.requireNonNull(motif, "motif");
        Objects.requireNonNull(deity, "deity");
        if (displayName == null || displayName.isBlank()) {
            displayName = language.generateRootName(id * 0x9E3779B97F4A7C15L ^ hearthRegion);
        }
    }

    public Culture(
            int id,
            String nameKey,
            LanguageTemplate language,
            ScriptType script,
            ArchStyle primaryStyle,
            ArchStyle secondaryStyle,
            Burial burial,
            Motif motif,
            Deity deity,
            long hearthRegion
    ) {
        this(id, nameKey, "", language, script, primaryStyle, secondaryStyle, burial, motif, deity, hearthRegion);
    }

    public Identifier identifier() {
        String slug = displayName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "");
        if (slug.isEmpty()) {
            slug = "culture_" + id;
        } else {
            slug = slug + "_" + id;
        }
        return Identifier.fromNamespaceAndPath("tcechoes", slug);
    }

    /**
     * Cultural distance d(a, b) in [0, 1] from language, script, and architecture differences (18 §5.1.4).
     */
    public double distanceTo(Culture other) {
        if (other == null || this.id == other.id) {
            return 0.0D;
        }
        double d = 0.0D;
        if (this.language != other.language) {
            d += 0.45D;
        }
        if (this.script != other.script) {
            d += 0.25D;
        }
        if (this.primaryStyle != other.primaryStyle) {
            d += 0.20D;
        }
        if (this.deity != other.deity) {
            d += 0.10D;
        }
        return StrictMath.min(1.0D, d);
    }
}
